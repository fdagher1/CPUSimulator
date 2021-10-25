package components;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import components.CPU;
import components.L1Cache;
import components.Memory;
import components.ROM;
import consoles.Console;
import consoles.TechnicianConsole;

/* The Main class instantiates the different components of the program (CPU, Memory, etc.), then loads the console window.
 * It also includes functions called by the various UI buttons.
 */
public abstract class Main {

	// Instantiate program components
	static Console console = new Console("CSCI 6461");
	public static TechnicianConsole technicianConsole = new TechnicianConsole("CSCI 6461 Technician Console");

	static Memory memory = new Memory();
	static L1Cache l1cache = new L1Cache();
	static ROM rom = new ROM();
	static CPU cpu = new CPU(console);

	// Load UI window
	public static void startConsole() {
		console.setVisible(true);
		console.haltConsole(true);
	}

	// Open technicians console
	public static void startTechnicianConsole() {
		technicianConsole.setVisible(true);
	}

	// Called by the console class when the IPL button is clicked
	public static void iplButtonClicked() {
		reset();

		// Load the content of rom into memory
		rom.ROMloader(memory, cpu, console, technicianConsole, l1cache);

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
		console.writeToOutput("ROM loaded into memory.");
	}

	// Called by the console class when the One Step button is clicked
	public static void oneStepButtonClicked() throws InterruptedException, FileNotFoundException {
		// Call function to MAIN one instruction from memory address PC
		if (console.isNotHalted) {
			executeOneStep();
		}

		// Console may have been halted when the step was executed so this checks for
		// that.
		if (console.isNotHalted) {
			// Update UI display
			console.updateUI(cpu, memory, l1cache);
			console.writeToOutput("Executed one instruction.");
		}

	}

	// Called by the console class when the user wants to run the program until a
	// HALT instruction is hit
	public static void runProgramButtonClicked() throws FileNotFoundException, InterruptedException {
		while (console.isNotHalted) {
			executeOneStep();
		}

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
		console.writeToOutput("Finished running program.");
	}

	// Called by the console class when the Load P1 button is clicked. This loads
	// the program that is in our program 1 file and loads it into memory
	public static void loadP1Clicked() throws IOException {
		reset();
		
		String programOneFileName = "Program_1.txt"; // Name of the file containing program 1

		// Java setup to be able to read file
		File inputFile = new File(programOneFileName);
		BufferedReader br = new BufferedReader(new FileReader(inputFile));

		// First nine memory spaces are blank
		for (int i = 0; i < 9; i++) {
			br.readLine();
		}

		cpu.MAR = 10;
		cpu.PC = 28;

		String instruction;
		while ((instruction = br.readLine()) != null) {
			// Ignoring blank lines
			if (instruction.trim().equals("")) {
				continue;
			}

			// memory.write(cpu.MAR, instruction.trim());
			l1cache.write(cpu.MAR, instruction.trim(), memory);
			cpu.MAR++;
		}

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
		console.writeToOutput("P1 Loaded into memory.");

		br.close();
	}

	// Called by the console class when the Load P2 button is clicked. This loads
	// the program that is in our program 2 file and loads it into memory
	public static void loadP2Clicked() throws IOException {
		reset();

		// Temporary for reading in characters from card reader to memory starting at
		// location 1000
		File inputFile = new File("Card_Reader.txt");
		BufferedReader br = new BufferedReader(new FileReader(inputFile));

		String instruction;
		int memoryLocationToStoreChar = 159;

		StringBuilder sentenceBuilder = new StringBuilder();

		while ((instruction = br.readLine()) != null) {
			for (char c : instruction.toUpperCase().toCharArray()) {

				// The user input characters are stored in memory location 2000-2047 so we do
				// not want to run into this. We want spot 2000 to be left blank (to indicate
				// the end of the input) so that is why
				// we halt when we get to 1999.
				if (memoryLocationToStoreChar >= 1999) {
					console.writeToOutput("Input from Card Reader too long.");
					console.haltConsole(true);
					br.close();
					return;
				}

				memory.write(memoryLocationToStoreChar,
						String.format("%16s", Integer.toBinaryString(c)).replace(" ", "0"));
				memoryLocationToStoreChar++;

				sentenceBuilder.append(c);

				if ((c == '.') || (c == '?') || (c == '!')) {
					console.appendToConsolePrinter(sentenceBuilder.toString(), false);
					sentenceBuilder = new StringBuilder();
				}

			}
		}

		br.close();

		String programTwoFileName = "Program_2.txt"; // Name of the file containing program 1

		// Java setup to be able to read file
		inputFile = new File(programTwoFileName);
		br = new BufferedReader(new FileReader(inputFile));

		cpu.MAR = 10;
		cpu.PC = 30;

		// First nine memory spaces are blank
		for (int i = 0; i < 9; i++) {
			br.readLine();
		}

		while ((instruction = br.readLine()) != null) {
			// Ignoring blank lines
			if (instruction.trim().equals("")) {
				continue;
			}

			memory.write(cpu.MAR, instruction.trim());
			cpu.MAR++;
		}

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
		console.writeToOutput("P2 Loaded into memory.");

		br.close();

	}

	// Execute one word from memory
	public static void executeOneStep() throws InterruptedException, FileNotFoundException {

		cpu.MAR = cpu.PC;
		cpu.MBR = l1cache.read(cpu.MAR, memory);
		cpu.IR = cpu.MBR;

		cpu.processInstruction(memory, l1cache);

		cpu.PC++;
	}

	// Called by the console class when the deposit button is clicked
	public static void depositButtonClicked(String value) {
		// memory.write(cpu.MAR, value);
		l1cache.write(cpu.MAR, value, memory);

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
		console.writeToOutput("Deposited one instruction.");
	}

	// Called by the console class when the Change PC button is clicked
	public static void changePCButtonClicked(String value) {
		console.writeToOutput("Changing PC value.");

		// Convert entered value to decimal, but if equal to zero, reject input
		int decValue = Integer.parseInt(value, 2);
		if (decValue == 0) {
			console.writeToOutput("Cannot set PC to zero.");
			return;
		}

		// Else set PC to that value
		cpu.PC = decValue;

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
	}

	// Called by the console class when the Change MAR button is clicked
	public static void changeMARButtonClicked(String value) {
		console.writeToOutput("Changing MAR Value.");

		// Convert entered value to decimal
		int decValue = Integer.parseInt(value, 2);

		// Set MAR to that value (and as a result, MBR)
		cpu.MAR = decValue;
		cpu.MBR = l1cache.read(cpu.MAR, memory);

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
	}

	// Method to reset the backend components of the machine
	private static void reset() {
		memory = new Memory();
		l1cache = new L1Cache();
		rom = new ROM();
		cpu = new CPU(console);

		console.getTextConsolePrinter().setText("");
		console.getTextStatus().setText("");
	}

	// Method to handle when the user wants to deposit a value from the toggles
	// directly into a GPR or Index register
	public static void depositToRegisterButtonClicked(ActionEvent e) {
		String buttonName = e.getActionCommand();
		String registerName = buttonName.split(" ")[2]; // For example the string 'Deposit to R0' we only want the R0

		if (registerName.charAt(0) == 'R') {
			// If the button is corresponding to a general purpose register
			int registerNumber = registerName.charAt(1) - 48; // Subtracting 48 to convert ASCII code to actual number
																// it represents
			cpu.R[registerNumber] = console.readToggleInput();
		} else if (registerName.charAt(0) == 'X') {
			// If the button is corresponding to a index register
			int registerNumber = registerName.charAt(1) - 48; // Subtracting 48 to convert ASCII code to actual number
																// it represents
			cpu.X[registerNumber] = console.readToggleInput();
		} else if (registerName.charAt(0) == 'F') {
			int registerNumber = registerName.charAt(2) - 48; // Subtracting 48 to convert ASCII code to actual number
																// it represents

			cpu.FPR[registerNumber] = console.readToggleInput();
		} else {
			console.getTextStatus().setText("ERROR: Depositing to register");
			return;
		}

		// Update UI display
		console.updateUI(cpu, memory, l1cache);
		console.writeToOutput("Deposited to " + registerName);
	}

	public static void faultOccured(int id) {

		// Store PC value in memory address 4 after converting it to 16 bit binary
		String pcaddress = Integer.toString(cpu.PC, 2);
		while (pcaddress.length() < 16) {
			pcaddress = "0" + pcaddress;
		}
		l1cache.write(4, pcaddress, memory);

		// Update MFR register with corresponding fault code and write to output
		if (id == 0) {
			cpu.MFR = "0001";
			console.writeToOutput("Fault: Can't write to reserved Address.");
		} else if (id == 1) {
			cpu.MFR = "0010";
		} else if (id == 2) {
			cpu.MFR = "0100";
			console.writeToOutput("Fault: Illegal Opcode.");
		} else if (id == 3) {
			cpu.MFR = "1000";
			console.writeToOutput("Fault: Address out of bounds.");
		}
		console.writeToOutput("Fault occured at PC: " + cpu.PC + ", and MAR: " + cpu.MAR);
		console.updateUI(cpu, memory, l1cache);

		// Load PC with content of memory address 1
		cpu.PC = Integer.parseInt(l1cache.read(1, memory), 2);

		// Halt console
		console.haltConsole(true);
	}
}
