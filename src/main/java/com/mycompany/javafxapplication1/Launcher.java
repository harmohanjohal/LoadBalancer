package com.mycompany.javafxapplication1;

/**
 * Entry point that delegates to {@link App#main(String[])}.
 * <p>
 * JavaFX requires module-path configuration when the main class extends
 * {@code Application}. This launcher class sidesteps that check so the
 * application can be started with a plain {@code java -cp} command.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        App.main(args);
    }
}
