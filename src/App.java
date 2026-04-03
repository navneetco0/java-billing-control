import authentication.LoginWindow;

public class App {
    public static void main(String[] args) {
        // macOS: set app name in dock before anything starts
        System.setProperty("apple.awt.application.name", "MyApp");

        javax.swing.SwingUtilities.invokeLater(() -> {
            LoginWindow lw = new LoginWindow();
            lw.connectDB(); // Connect on EDT is fine for init; or use SwingWorker below
        });
    }
}