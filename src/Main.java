import view.MenuFrame;

import javax.swing.*;

public class Main {
    static void main() {
        SwingUtilities.invokeLater(() -> {
            MenuFrame mainFrame = new MenuFrame(1100, 810);
            mainFrame.setVisible(true);
        });
    }
}
