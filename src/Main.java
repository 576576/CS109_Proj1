import view.MenuFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MenuFrame mainFrame = new MenuFrame(1100, 810);
            mainFrame.setVisible(true);
        });
    }
}
