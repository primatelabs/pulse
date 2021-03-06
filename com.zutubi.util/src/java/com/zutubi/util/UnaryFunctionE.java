package com.zutubi.util;

/**
 * Generic unary function which may produce an exception.  Takes one input
 * and transforms it to one output.
 */
public interface UnaryFunctionE<T, U, E extends Throwable>
{
    U process(T t) throws E;
}
