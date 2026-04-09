import authentication.LoginWindow;

public class App {
    public static void main(String[] args) {
        // macOS: set app name in dock before anything starts
        System.setProperty("apple.awt.application.name", "MyApp");

        javax.swing.SwingUtilities.invokeLater(() -> {
            new LoginWindow();
        });
    }
}