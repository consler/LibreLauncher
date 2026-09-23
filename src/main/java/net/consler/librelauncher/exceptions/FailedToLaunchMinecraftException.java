package net.consler.librelauncher.exceptions;

public class FailedToLaunchMinecraftException extends RuntimeException
{
    public FailedToLaunchMinecraftException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public FailedToLaunchMinecraftException(String message) {
        super(message);
    }
}
