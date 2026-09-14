package net.consler.librelauncher.exceptions;

public class FxmlLoadException extends RuntimeException
{
    public FxmlLoadException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public FxmlLoadException(String message)
    {
        super(message);
    }
}
