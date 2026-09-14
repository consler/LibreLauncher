package net.consler.librelauncher;

import net.consler.librelauncher.ui.client.ClientApplication;
import dev.dirs.BaseDirectories;
import javafx.application.Application;

import java.io.File;

public class Main
{

    public static final File APPDATA_DIR = new File(BaseDirectories.get().configDir, "LibreLauncher");

    static void main(String[] args)
    {
        Application.launch(ClientApplication.class, args);
    }
}
