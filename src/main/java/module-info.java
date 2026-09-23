module net.consler.librelauncher
{
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires javafx.web;
    requires javafx.swing;
    requires org.jetbrains.annotations;
    requires atlantafx.base;
    requires com.google.gson;
    requires dev.dirs;
    requires java.management;
    requires jdk.management;
    requires librelauncherlib;

    opens net.consler.librelauncher to javafx.fxml;
    exports net.consler.librelauncher;
    exports net.consler.librelauncher.ui.client;
    exports net.consler.librelauncher.ui.instance.manager;
    exports net.consler.librelauncher.ui.instance.create;
    exports net.consler.librelauncher.ui.instance.manager.specific;
    exports net.consler.librelauncher.ui.settings;
    opens net.consler.librelauncher.ui.client to javafx.fxml;
    opens net.consler.librelauncher.ui.instance.manager to javafx.fxml;
    opens net.consler.librelauncher.ui.instance.create to javafx.fxml;
    opens net.consler.librelauncher.ui.settings to javafx.fxml;
    opens net.consler.librelauncher.ui.settings.categories to javafx.fxml;
    opens net.consler.librelauncher.ui.instance.manager.specific to javafx.fxml;
}