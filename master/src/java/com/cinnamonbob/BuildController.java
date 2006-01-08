package com.cinnamonbob;

import com.cinnamonbob.core.Bootstrapper;
import com.cinnamonbob.core.BuildException;
import com.cinnamonbob.core.event.AsynchronousDelegatingListener;
import com.cinnamonbob.core.event.Event;
import com.cinnamonbob.core.event.EventListener;
import com.cinnamonbob.core.event.EventManager;
import com.cinnamonbob.core.model.Changelist;
import com.cinnamonbob.core.model.RecipeResult;
import com.cinnamonbob.core.model.Revision;
import com.cinnamonbob.core.util.TreeNode;
import com.cinnamonbob.events.build.BuildCommencedEvent;
import com.cinnamonbob.events.build.BuildCompletedEvent;
import com.cinnamonbob.events.build.RecipeEvent;
import com.cinnamonbob.model.*;
import com.cinnamonbob.scm.SCMException;
import com.cinnamonbob.scm.SCMServer;
import com.cinnamonbob.util.logging.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class BuildController implements EventListener
{
    private static final Logger LOG = Logger.getLogger(BuildController.class);

    private Project project;
    private BuildSpecification specification;
    private EventManager eventManager;
    private BuildManager buildManager;
    private RecipeQueue queue;
    private RecipeResultCollector collector;
    private BuildTree tree;
    private BuildResult buildResult;
    private AsynchronousDelegatingListener asyncListener;
    private List<TreeNode<RecipeController>> executingControllers = new LinkedList<TreeNode<RecipeController>>();

    public BuildController(Project project, BuildSpecification specification, EventManager eventManager, BuildManager buildManager, RecipeQueue queue, RecipeResultCollector collector)
    {
        this.project = project;
        this.specification = specification;
        this.eventManager = eventManager;
        this.buildManager = buildManager;
        this.queue = queue;
        this.collector = collector;
        this.asyncListener = new AsynchronousDelegatingListener(this);
    }

    public void run()
    {
        createBuildTree();

        MasterBuildPaths paths = new MasterBuildPaths();
        File buildDir = paths.getBuildDir(project, buildResult);
        buildResult.commence(buildDir);
        buildManager.save(buildResult);

        // We handle this event ourselves: this ensures that all processing of
        // the build from this point forth is handled by the single thread in
        // our async listener.  Basically, given events could be coming from
        // anywhere, even for different builds, it is much safer to ensure we
        // *only* use that thread after we have registered the listener.
        eventManager.register(asyncListener);
        eventManager.publish(new BuildCommencedEvent(this, buildResult));
    }

    public BuildTree createBuildTree()
    {
        tree = new BuildTree();

        TreeNode<RecipeController> root = tree.getRoot();
        buildResult = new BuildResult(project, buildManager.getNextBuildNumber(project));
        buildManager.save(buildResult);
        configure(root, buildResult.getRoot(), specification.getRoot());

        return tree;
    }

    private ScmBootstrapper createBuildBootstrapper()
    {
        ScmBootstrapper bootstrapper = new ScmBootstrapper();

        for (Scm scm : project.getScms())
        {
            try
            {
                SCMServer server = scm.createServer();
                Revision latestRevision = server.getLatestRevision();
                bootstrapper.add(new ScmCheckoutDetails(scm, latestRevision));

                // collect scm changes to be added to the build results.
                List<Changelist> scmChanges = null;

                try
                {
                    List<BuildResult> previousBuildResults = buildManager.getLatestBuildResultsForProject(project, 2);

                    if (previousBuildResults.size() == 2)
                    {
                        BuildScmDetails previousScmDetails = previousBuildResults.get(1).getScmDetails(scm.getId());
                        if (previousScmDetails != null)
                        {
                            Revision previousRevision = previousScmDetails.getRevision();
                            if (previousRevision != null)
                            {
                                scmChanges = server.getChanges(previousRevision, latestRevision, "");
                            }
                        }
                    }
                }
                catch (SCMException e)
                {
                    // TODO: need to report this failure to the user. However, this is not fatal to the current build
                    LOG.warning("Unable to retrieve changelist details from SCM server. ", e);
                }

                BuildScmDetails scmDetails = new BuildScmDetails(scm.getName(), latestRevision, scmChanges);
                buildResult.addScmDetails(scm.getId(), scmDetails);
            }
            catch (SCMException e)
            {
                throw new BuildException("Could not retrieve latest revision from SCM '" + scm.getName() + "'", e);
            }
        }

        buildManager.save(buildResult);

        return bootstrapper;
    }

    private void run(Bootstrapper bootstrapper, TreeNode<RecipeController> node)
    {
        executingControllers.add(node);
        node.getData().initialise(bootstrapper);
    }

    private void configure(TreeNode<RecipeController> rcNode, RecipeResultNode resultNode, BuildSpecificationNode specNode)
    {
        for (BuildSpecificationNode node : specNode.getChildren())
        {
            BuildStage stage = node.getStage();
            RecipeResult recipeResult = new RecipeResult(stage.getRecipe());
            RecipeResultNode childResultNode = new RecipeResultNode(recipeResult);
            resultNode.addChild(childResultNode);
            buildManager.save(resultNode);

            MasterBuildPaths paths = new MasterBuildPaths();
            recipeResult.setOutputDir(paths.getRecipeDir(project, buildResult, recipeResult.getId()).getAbsolutePath());

            RecipeRequest recipeRequest = new RecipeRequest(recipeResult.getId(), project.getBobFile(), stage.getRecipe());
            RecipeDispatchRequest dispatchRequest = new RecipeDispatchRequest(stage.getHostRequirements(), recipeRequest);
            RecipeController rc = new RecipeController(childResultNode, dispatchRequest, collector, queue, buildManager);
            TreeNode<RecipeController> child = new TreeNode<RecipeController>(rc);
            rcNode.add(child);
            configure(child, childResultNode, node);
        }
    }

    public void handleEvent(Event evt)
    {
        try
        {
            if (evt instanceof BuildCommencedEvent)
            {
                BuildCommencedEvent e = (BuildCommencedEvent) evt;
                if (e.getResult() == buildResult)
                {
                    handleBuildCommenced();
                }
            }
            else
            {
                RecipeEvent e = (RecipeEvent) evt;
                handleRecipeEvent(e);
            }
        }
        catch (BuildException e)
        {
            buildResult.error(e);
            completeBuild();
        }
        catch (Exception e)
        {
            LOG.severe(e);
            buildResult.error("Unexpected error: " + e.getMessage());
            completeBuild();
        }
    }

    private void handleBuildCommenced()
    {
        // It is important that this directory is created *after* the build
        // result is commenced and saved to the database, so that the
        // database knows of the possibility of some other persistent
        // artifacts, even if an error occurs very early in the build.
        File buildDir = new File(buildResult.getOutputDir());
        if (!buildDir.mkdirs())
        {
            throw new BuildException("Unable to create build directory '" + buildDir.getAbsolutePath() + "'");
        }

        tree.prepare(buildResult);

        // execute the first level of recipe controllers...
        Bootstrapper initialBootstrapper = createBuildBootstrapper();
        for (TreeNode<RecipeController> node : tree.getRoot())
        {
            run(initialBootstrapper, node);
        }
    }

    private void handleRecipeEvent(RecipeEvent e)
    {
        RecipeController controller = null;
        TreeNode<RecipeController> foundNode = null;

        for (TreeNode<RecipeController> node : executingControllers)
        {
            controller = node.getData();
            if (controller.handleRecipeEvent(e))
            {
                foundNode = node;
                break;
            }
        }

        if (foundNode != null)
        {
            if (controller.isFinished())
            {
                controller.collect(buildResult);
                executingControllers.remove(foundNode);

                if (controller.succeeded())
                {
                    for (TreeNode<RecipeController> child : foundNode.getChildren())
                    {
                        run(controller.getChildBootstrapper(), child);
                    }
                }
                else
                {
                    buildResult.failure("Recipe '" + controller.getRecipeName() + "@" + controller.getRecipeHost() + "' failed");
                    buildManager.save(buildResult);
                }
            }

            if (executingControllers.size() == 0)
            {
                completeBuild();
            }
        }
    }

    private void completeBuild()
    {
        buildResult.abortUnfinishedRecipes();
        tree.cleanup(buildResult);
        buildResult.complete();
        buildManager.save(buildResult);
        eventManager.unregister(asyncListener);
        eventManager.publish(new BuildCompletedEvent(this, buildResult));
        asyncListener.stop();
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{BuildCommencedEvent.class, RecipeEvent.class};
    }
}
