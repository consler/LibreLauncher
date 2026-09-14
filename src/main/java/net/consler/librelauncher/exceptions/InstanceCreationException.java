package net.consler.librelauncher.exceptions;

public class InstanceCreationException extends RuntimeException
{
    public InstanceCreationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InstanceCreationException(String message)
    {
        super(message);
    }
}
