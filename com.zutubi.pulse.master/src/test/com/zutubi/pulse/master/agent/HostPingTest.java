package com.zutubi.pulse.master.agent;

import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.servercore.agent.PingStatus;
import com.zutubi.pulse.servercore.services.HostStatus;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class HostPingTest extends PulseTestCase
{
    private static final String HOST_LOCATION = "testhost";
    private static final String MASTER_LOCATION = "test location";

    private int buildNumber = 10;
    private Host host;
    private HostService hostService;

    public void setUp() throws Exception
    {
        super.setUp();

        host = mock(Host.class);
        stub(host.getLocation()).toReturn(HOST_LOCATION);
        hostService = mock(HostService.class);
    }

    public void testSuccessfulPing() throws Exception
    {
        stub(hostService.ping()).toReturn(buildNumber);
        HostStatus status = new HostStatus(PingStatus.IDLE, true);
        stub(hostService.getStatus(MASTER_LOCATION)).toReturn(status);

        assertSame(status, doPing());
    }

    public void testFailedPing() throws Exception
    {
        stub(hostService.ping()).toThrow(new RuntimeException("bang"));

        HostPing ping = new HostPing(host, hostService, buildNumber, MASTER_LOCATION);
        assertEquals(new HostStatus(PingStatus.OFFLINE, "Exception: 'java.lang.RuntimeException'. Reason: bang"), ping.call());
    }

    public void testFailedGetStatus() throws Exception
    {
        stub(hostService.ping()).toReturn(buildNumber);
        stub(hostService.getStatus(MASTER_LOCATION)).toThrow(new RuntimeException("oops"));

        assertEquals(new HostStatus(PingStatus.OFFLINE, "Exception: 'java.lang.RuntimeException'. Reason: oops"), doPing());
    }

    public void testVersionMismatch() throws Exception
    {
        stub(hostService.ping()).toReturn(buildNumber - 1);

        assertEquals(new HostStatus(PingStatus.VERSION_MISMATCH), doPing());
    }

    private HostStatus doPing()
    {
        HostPing ping = new HostPing(host, hostService, buildNumber, MASTER_LOCATION);
        return ping.call();
    }
}
