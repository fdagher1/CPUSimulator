package consoles;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

// This holds methods and properties that pertain to the design of our console. 
// This is separated into its own class so that multiple different console 
// can reference it.
public class ConsoleStyleProperties {

	// Design colors
	// These are aggregated here so we can easily change our color scheme. The
	// current
	// name represents its current color but if we want to change the color then we
	// can just refactor the name.
	protected static Color yellow = new Color(218, 165, 32);
	protected static Color textFieldYellow = Color.YELLOW;
	protected static Color darkGray = Color.DARK_GRAY;
	protected static Color grayedOutColor = Color.DARK_GRAY;
	protected static Color white = Color.WHITE;
	protected static Color black = Color.BLACK;

	// ****
	// These methods apply the design properties we want for certain elements on
	// our consoles.
	// ****
	protected static void applyButtonProperties(JButton button) {
		button.setOpaque(true);
		button.setBackground(ConsoleStyleProperties.yellow);
		button.setForeground(ConsoleStyleProperties.white);
		button.setBorderPainted(false);
		// button.setFont(new Font("Arial", Font.PLAIN, 10));
	}

	// The deposit buttons next to the index and GPR registers needs to be a
	// different font
	protected static void applyDepositButtonProperties(JButton button) {
		applyButtonProperties(button);
		button.setFont(new Font("Arial", Font.PLAIN, 9));
	}

	protected static void applyLabelProperties(JLabel label) {
		label.setForeground(ConsoleStyleProperties.white);
	}

	protected static void applyTextFieldProperties(JTextField textField) {
		textField.setForeground(ConsoleStyleProperties.textFieldYellow);
		textField.setBackground(ConsoleStyleProperties.black);
		textField.setEditable(false);
	}

	// If the textField is currently grayed out it will be un-grayed out and vice
	// versa
	protected static void grayOrUnGrayTextField(JTextField textField) {
		if (textField.getBackground() == grayedOutColor) {
			textField.setBackground(ConsoleStyleProperties.black);
			textField.setEditable(true);
			textField.setCaretColor(ConsoleStyleProperties.textFieldYellow); // Changing blinking cursor color
		} else {
			textField.setBackground(grayedOutColor);
			textField.setEditable(false);
		}
	}

	protected static void applyTextAreaProperties(JTextArea textArea) {
		textArea.setForeground(ConsoleStyleProperties.textFieldYellow);
		textArea.setBackground(ConsoleStyleProperties.black);
	}

}
