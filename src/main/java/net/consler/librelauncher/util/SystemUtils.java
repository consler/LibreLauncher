package net.consler.librelauncher.util;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SystemUtils
{
    public static void openDirectory(File file) throws IOException
    {
        Desktop.getDesktop().open(file);
    }
}
