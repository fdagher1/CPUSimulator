package components;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.JOptionPane;

import consoles.Console;

/* The CPU class is a representation of the computer's central processing unit. It contains all the registers used by the CISC, 
 * as well as the decoding and processing of the instructions getting executed by the program.
 */
public class CPU {

	// Used for inputting from and outputing to the card reader (which is simulated
	// by us as a txt file)
	String cardReaderFileName = "Card_Reader.txt";

	// Declare the different registers
	public int PC; // Program Counter, in decimal values
	public String CC; // Condition Code, 4 bit string (ex: "0001"), each bit representing the
						// following in this order: OVERFLOW, UNDERFLOW, DIVZERO, EQUALORNOT
	public int MAR; // Memory Address Register, in decimal values
	public String MBR; // Memory Buffer Register, 12 bit string to be sent/retrieved to/from memory
	public String IR; // Instruction Register, 16 bit string to be executed
	public String[] R = new String[4]; // Four General Purpose Registers, each 16 bit strings
	public String[] X = new String[4]; // Three Index Registers, each 16 bit string holding the base address, X[0] will
										// be ignored
	public String[] FPR = new String[2]; // Two floating point registers, each 16 bits in length
	public String MFR; // Machine Fault Register, 4 bit String containing the ID code for the machine
						// fault

	public String opcode;
	public int GPR;
	public int IX;
	public int I;
	public String address;

	public Console console; // reference to console to we can get input from user

	// Constructor
	public CPU(Console console) {
		this.console = console;
		clearCPURegisters();
	}

	// Function to clear all CPU registers, called by the constructors, as well as
	// by the IPL button
	public void clearCPURegisters() {
		// Clear all registers
		PC = 0;
		CC = "";
		MAR = 0;
		MBR = "0000000000000000";
		IR = "0000000000000000";
		Arrays.fill(R, "0000000000000000");
		Arrays.fill(X, "0000000000000000");
		Arrays.fill(FPR, "0000000000000000");
		MFR = "0000";
		opcode = "000000";
		GPR = 0;
		IX = 0;
		I = 0;
		address = "00000";
	}

	// This method breaks down the content of the IR register, placing each part in
	// its corresponding variable
	// Then calls interpretCode method to process the opcode
	// It has memory as a parameter because the memory object is not accessible from
	// this class
	public void processInstruction(Memory memory, L1Cache l1cache) throws InterruptedException, FileNotFoundException {
		opcode = IR.substring(0, 6);
		GPR = Integer.parseInt(IR.substring(6, 8), 2);
		IX = Integer.parseInt(IR.substring(8, 10), 2);
		I = Integer.parseInt(IR.substring(10, 11), 2);
		address = IR.substring(11, 16);

		interpretOpcode(memory, l1cache);
	}

	// Method to interpret the content of opcode
	// It has memory as a parameter because the memory object is not accessible from
	// this class
	public void interpretOpcode(Memory memory, L1Cache l1cache) throws InterruptedException, FileNotFoundException {

		switch (opcode) {

		// LDR (01) - Load register from memory
		case "000001":
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			R[GPR] = MBR;
			break;

		// STR (02) - Store register to memory
		case "000010":
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = R[GPR];
			l1cache.write(MAR, MBR, memory);
			break;

		// LDA (03) - Load register with address
		case "000011":
			R[GPR] = "00000000000" + address;
			break;

		// LDX (33) - Load index register from memory
		case "100001":
			// As suggested by the professor, this instruction will not use the IX for
			// calculating the EA
			// So X[IX] will be backed up then cleared during EA calculation
			String tempXIXforLDX = X[IX];
			X[IX] = "0000000000000000";

			MAR = calculateEffectiveAddress(memory, l1cache);

			// Restore the X[IX] value since EA is now calculated
			X[IX] = tempXIXforLDX;

			MBR = l1cache.read(MAR, memory);
			X[IX] = MBR;
			break;

		// STX (34) - Store index register to memory
		case "100010":
			// As suggested by the professor, this instruction will not use the IX for
			// calculating the EA
			// So X[IX] will be backed up then cleared during EA calculation
			String tempXIXforSDX = X[IX];
			X[IX] = "0000000000000000";

			MAR = calculateEffectiveAddress(memory, l1cache);

			// Restore the X[IX] value since EA is now calculated
			X[IX] = tempXIXforSDX;

			MBR = X[IX];
			l1cache.write(MAR, MBR, memory);
			break;

		// JZ (8) - Jump if Zero
		case "001000":
			if (R[GPR].equals("0000000000000000")) {
				// Subtracting 1 because at the end of Main.executeOneStep() the PC gets
				// automatically incremented by 1.
				PC = calculateEffectiveAddress(memory, l1cache) - 1;
			}
			break;

		// JNE (9) - Jump if Not Equal
		case "001001":
			if (!R[GPR].equals("0000000000000000")) {
				// Subtracting 1 because at the end of Main.executeOneStep() the PC gets
				// automatically incremented by 1.
				PC = calculateEffectiveAddress(memory, l1cache) - 1;		
			}

			break;

		// JCC (10) - Jump if Condition Code
		case "001010":
			// if the fourth bit is set and it is asking for the forth bit
			if (Integer.parseInt(CC) >= 8 && GPR == 3) {
				PC = calculateEffectiveAddress(memory, l1cache);
				// if the third bit is set and it is asking for the third bit
			} else if (Integer.parseInt(CC) >= 4 && GPR == 2) {
				PC = calculateEffectiveAddress(memory, l1cache);
				// if the second bit is set and it is asking for the second bit
			} else if (Integer.parseInt(CC) >= 2 && GPR == 1) {
				PC = calculateEffectiveAddress(memory, l1cache);
				// if the first bit is set and it is asking for the second bit
			} else if (Integer.parseInt(CC) >= 1 && GPR == 0) {

				PC = calculateEffectiveAddress(memory, l1cache);
			}
			break;

		// JMA (11) - Unconditional Jump to Address
		case "001011":
			// Subtracting 1 because at the end of Main.executeOneStep() the PC gets
			// automatically incremented by 1.
			PC = calculateEffectiveAddress(memory, l1cache) - 1;
			break;

		// JSR (12) - Jump and Save Return Address
		// TODO determine the arguments
		case "001100":
			R[3] = Integer.toBinaryString((PC + 1));
			PC = calculateEffectiveAddress(memory, l1cache);
			// R0 stores arguments?
			break;

		// RFS (13) - Return from subroutine
		case "001101":
			R[0] = String.format("%16s", address).replace(" ", "0");
			PC = Integer.parseInt(R[3], 2);
			break;

		// SOB (14) - Subtract one and branch
		case "001110":
			int value = (Integer.parseInt(R[GPR], 2) - 1);
			R[GPR] = String.format("%16s", Integer.toBinaryString(value)).replace(" ", "0");
			if (value > 0) {
				PC = calculateEffectiveAddress(memory, l1cache);
			}
			break;

		// JGE (15) - Jump Greater than or equal to
		case "001111":
			int value2 = 0;

			if (R[GPR].charAt(0) == '1') {
				// This means it is a negative number stored in twos complement that we need as
				// a decimal
				short res = (short) Integer.parseInt(R[GPR], 2); // https://stackoverflow.com/a/15837952
				value2 = (int) res;
			} else {
				value2 = (Integer.parseInt(R[GPR], 2));
			}

			if (value2 >= 0) {
				// Subtracting 1 because at the end of Main.executeOneStep() the PC gets
				// automatically incremented by 1.
				PC = calculateEffectiveAddress(memory, l1cache) - 1;
			}
			break;

		// AMR (04) - Add Memory to Register
		case "000100":
			// get the value of the register
			int value3 = (Integer.parseInt(R[GPR], 2));
			// get the value of the memory
			MAR = calculateEffectiveAddress(memory, l1cache);
			// MBR = memory.read(MAR);
			MBR = l1cache.read(MAR, memory);
			// add the two and store into register
			int add = value3 + Integer.parseInt(MBR, 2);
			R[GPR] = String.format("%16s", add).replace(" ", "0");
			// set overflow bit if too big
			if (add > 32767) {
				R[GPR] = R[GPR].substring(16);
				CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
						.replace(" ", "0");
			}
			break;

		// SMR (05) - Subtract Memory from Register
		case "000101":
			// get the value of the register
			int value4 = Integer.parseInt(R[GPR], 2);
			// get the value of the memory
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			// subtract the two and store into register
			int subtract = value4 - Integer.parseInt(MBR, 2);
			if (subtract < 0) {
				R[GPR] = String.format("%16s", Integer.toBinaryString(subtract)).replace(" ", "0").substring(16, 32);
			} else {
				R[GPR] = String.format("%16s", Integer.toBinaryString(subtract)).replace(" ", "0");
			}
			// set underflow if too small
			if (subtract < -32768) {
				R[GPR] = R[GPR].substring(16);
				CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
						.replace(" ", "0");
			}
			break;

		// AIR (06) - Add Immediate Register
		case "000110":
			// get value of the register
			int value5 = Integer.parseInt(R[GPR], 2);
			if (Integer.parseInt(address, 2) == 0) {
				break;
			}
			// add the register to the address value
			int add2 = value5 + Integer.parseInt(address, 2);
			R[GPR] = String.format("%16s", Integer.toBinaryString(add2)).replace(" ", "0");
			// set overflow bit if too big
			if (add2 > 32767) {
				R[GPR] = R[GPR].substring(16);
				CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
						.replace(" ", "0");
			}
			break;

		// SIR (07) - Subtract Immediate from Register
		case "000111":
			// get value of the register
			int value6 = Integer.parseInt(R[GPR], 2);
			if (Integer.parseInt(address, 2) == 0) {
				break;
			}
			// subtract the register to the address value
			int subtract2 = value6 - Integer.parseInt(address, 2);
			R[GPR] = String.format("%16s", subtract2).replace(" ", "0");
			// set underflow bit if too small
			if (subtract2 < -32768) {
				R[GPR] = R[GPR].substring(16);
				CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
						.replace(" ", "0");
			}

			break;

		// MLT (16) - Multiply Register by Register
		case "010000":
			// check that the registers are either 0 or 2
			if (GPR == 0 || GPR == 2 && IX == 0 || IX == 2) {

				if (GPR == 0 || GPR == 2 && IX == 0 || IX == 2) {

					int value7 = Integer.parseInt(R[GPR], 2);
					int value8 = Integer.parseInt(R[IX], 2);
					// multiply the contents of the registers
					int mult = value7 * value8;
					String multString = Integer.toBinaryString(mult);
					// set first 16 bits in RX, and the second 16 bits in RX + 1
					if (mult > 32767) {
						R[GPR] = multString.substring((multString.length() - 16), multString.length());
						R[GPR + 1] = String
								.format("%16s", Integer.toBinaryString(mult).substring(0, (multString.length() - 16)))
								.replace(" ", "0");
						// if overflow set the overflow bit
						if (mult > 2147483647) {
							R[GPR + 1] = String.format("%16s", Integer.toBinaryString(mult).substring(0, 15))
									.replace(" ", "0");
							CC = String
									.format("%4s",
											Integer.toBinaryString(
													Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
									.replace(" ", "0");
						}
					}
					// if only 16 bits set into RX and set RX + 1 as 0
					else {
						R[GPR] = String.format("%16s", Integer.toBinaryString(mult)).replace(" ", "0");
						R[GPR + 1] = "0000000000000000";
					}
				}
			}
			break;

		// DVD (17) - Divide Register by Register
		case "010001":
			// check that the registers are either 0 or 2
			if (GPR == 0 || GPR == 2 && IX == 2 || IX == 0) {
				// if DIVBYZERO do nothing and set the DIVBYZERO flag
				if (Integer.parseInt(R[IX]) == 0) {
					CC = String
							.format("%4s",
									Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0100", 2)))
							.replace(" ", "0");
					break;
				}
				int value9 = Integer.parseInt(R[GPR], 2);
				int value10 = Integer.parseInt(R[IX], 2);
				// divide the values and store the value into rx and set the remainder into rx +
				// 1
				R[GPR] = String.format("%16s", Integer.toBinaryString((value9 / value10))).replace(" ", "0");
				R[GPR + 1] = String.format("%16s", Integer.toBinaryString((value9 % value10))).replace(" ", "0");
			}
			break;

		// TRR (18) - Test the Equality of Register and Register
		case "010010":
			int value11 = Integer.parseInt(R[GPR], 2);
			int value12 = Integer.parseInt(R[IX], 2);
			// if the values are equal then set the equalornot flag to 1
			if (value11 == value12) {
				CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("1000", 2)))
						.replace(" ", "0");
			}
			// if the values are not equal then set the equalornot flag to 0
			else {
				CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) & Integer.parseInt("0111", 2)))
						.replace(" ", "0");

			}
			break;

		// AND (19) - Logical AND of Register and Register
		case "010011":
			R[GPR] = String
					.format("%16s", Integer.toBinaryString(Integer.parseInt(R[GPR], 2) & Integer.parseInt(R[IX], 2)))
					.replace(" ", "0");
			break;

		// ORR (20) - Logical OR of Register and Register
		case "010100":
			R[GPR] = String
					.format("%16s", Integer.toBinaryString(Integer.parseInt(R[GPR], 2) | Integer.parseInt(R[IX], 2)))
					.replace(" ", "0");
			break;

		// NOT (21) - Logical NOT of Register and Register
		case "010101":
			if (R[GPR].charAt(0) == '1') {
				// If it is a negative number in twos complement
				R[GPR] = String.format("%16s", Integer.toBinaryString(~Integer.parseInt(R[GPR], 2))).replace(" ", "0")
						.substring(16, 32);
			} else {
				R[GPR] = String.format("%16s", Integer.toBinaryString(~Integer.parseInt(R[GPR], 2))).replace(" ", "0");
			}
			break;

		// SRC (25) - Shift Register by Count
		case "011001":
			if (Integer.parseInt(address, 2) == 0) {
				break;
			}
			// right shift by arithmetic
			if (IX == 0) {
				R[GPR] = String
						.format("%16s",
								Integer.toBinaryString(Integer.parseInt(R[GPR], 2) >> Integer.parseInt(address, 2)))
						.replace(" ", "0");
			}
			// left shift by arithmetic
			else if (IX == 1) {
				String leftShiftA = Integer.toBinaryString(Integer.parseInt(R[GPR], 2) << Integer.parseInt(address, 2));
				R[GPR] = leftShiftA.substring(Integer.parseInt(address, 2));
			}
			// right shift by logic
			else if (IX == 2) {
				R[GPR] = String
						.format("%16s",
								Integer.toBinaryString(Integer.parseInt(R[GPR], 2) >>> Integer.parseInt(address, 2)))
						.replace(" ", "0");
			}
			// left shift by logic
			else {
				String leftShiftL = Integer.toBinaryString(Integer.parseInt(R[GPR], 2) << Integer.parseInt(address, 2));
				R[GPR] = leftShiftL.substring(Integer.parseInt(address, 2));
			}
			break;

		// RRC (26) - Rotate Register by Count
		case "011010":
			if (Integer.parseInt(address, 2) == 0) {
				break;
			}
			String rightRotate;
			String leftRotate;
			// right shift by arithmetic
			if (IX == 0) {
				rightRotate = R[GPR].substring(0, (R[GPR].length() - Integer.parseInt(address, 2)));
				R[GPR] = R[GPR].substring(R[GPR].length() - Integer.parseInt(address, 2)) + rightRotate;
			}
			// left shift by arithmetic
			else if (IX == 1) {
				leftRotate = R[GPR].substring(Integer.parseInt(address, 2));
				R[GPR] = leftRotate + R[GPR].substring(0, Integer.parseInt(address, 2));
			}
			// right shift by logic
			else if (IX == 2) {
				rightRotate = R[GPR].substring(0, (R[GPR].length() - Integer.parseInt(address, 2)));
				R[GPR] = R[GPR].substring(R[GPR].length() - Integer.parseInt(address, 2)) + rightRotate;
			}
			// left shift by logic
			else {
				leftRotate = R[GPR].substring(Integer.parseInt(address, 2));
				R[GPR] = leftRotate + R[GPR].substring(0, Integer.parseInt(address, 2));
			}
			break;

		// IN (49) - Input Character To Register from Device
		// TODO make GUI changes for keyboard, printer, etc then update code to reflect
		case "110001":
			int devID = Integer.parseInt(address, 2);

			// Prompt the user for input
			String input = "";
			int inputInt = 0;

			if (devID == 0) {
				// User wants to input from the console keyboard
				input = JOptionPane.showInputDialog("Console is Requesting Input: ");

				// User does not input anything
				if (input == null) {
					input = "0";
				} else {
					// Check and see if the String input by the user is a number
					try {
						// If so convert that number represented as a String to an int
						inputInt = (Integer.parseInt(input.trim()));
					} catch (NumberFormatException nfe) {
						// Make sure the user only entered one character
						if (input.length() > 1) {
							JOptionPane.showMessageDialog(null, "ERROR: Can only input one character");
							console.haltConsole(true); // Halt the machine
							break;
						}

						// If it is not an int and just a character then inputInt will be the ASCII
						// value of that character
						inputInt = (int) input.charAt(0);
					}
				}
			} else if (devID == 1) {
				// User wants to input from the console printer, which does not make sense
				JOptionPane.showMessageDialog(null, "ERROR: Can't read from DEVID = 1 (Console Printer)");
			} else if (devID == 2) {
				// Java setup to be able to read file
				File inputFile = new File(cardReaderFileName);
				BufferedReader br = new BufferedReader(new FileReader(inputFile));

				// We will just take the first line of the card reader as the input
				try {
					input = br.readLine();
					inputInt = Integer.parseInt(input, 2);
				} catch (IOException e) {
					// Catching the error if the buffered reader throws one
					JOptionPane.showMessageDialog(null, "ERROR: Can't read line from Card Reader.");
					break;
				}
				try {
					br.close();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null,
							"ERROR when trying to close buffered reader in the IN instruction.");
				}
			} else if (devID == 3) {
				// When the users specifies a device ID of 3 our machine will read from the
				// toggles
				input = console.readToggleInput();
				inputInt = Integer.parseInt(input, 2);
			} else if (devID > 3) {
				// Our machine currently does not support device ID's greater than 3
				JOptionPane.showMessageDialog(null, "IN instruction does not currently support devID > 3");
				break;
			}

			if (inputInt > 32767) {
				// Checking for overflow
				CC = "0";
				break;
			} else {
				R[GPR] = String.format("%16s", Integer.toBinaryString(inputInt)).replace(" ", "0");
			}

			break;

		// OUT (50) - Output Character To Device from Register
		// TODO after GUI changes update this
		case "110010":
			devID = Integer.parseInt(address, 2);

			if (devID == 1) {
				// Write to new line of the console printer (as binary number).
				console.appendToConsolePrinter(R[GPR], false);
			} else if (devID == 0) {
				// Cannot write to console keyboard
				JOptionPane.showMessageDialog(null, "ERROR: Can't write to console keyboard");
			} else if (devID == 2) {
				// Write to the card reader. This overwrites whatever is already in the reader.
				// Java setup to be able to write to file
				try {
					FileWriter outputFileWriter = new FileWriter(cardReaderFileName);
					outputFileWriter.write(R[GPR]);
					outputFileWriter.close();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "ERROR: Can't setup file writer in OUT instruction");
				}
			} else if (devID == 3) {
				// Write to new line of console printer (as ASCII character)
				value = Integer.parseInt(R[GPR], 2);
				console.appendToConsolePrinter(Character.toString((char) value), false);
			}else if(devID == 4) {
				// Write in line of console printer (as ASCII character)
				value = Integer.parseInt(R[GPR], 2);
				console.appendToConsolePrinter(Character.toString((char) value), true);
			}else {
				// Does not currently support more devID's
				JOptionPane.showMessageDialog(null, "ERROR: DEVID not supported in output");
			}

			break;

		// TRAP (30) - TRAP code
		case "011110":
			// store next PC in memory spot 2
			memory.write(2, String.format("%16s", (PC + 1)).replace(" ", "0"));
			// go to memory spot 0 and get address
			MAR = 0;
			MBR = memory.read(MAR);
			// if trap code illegal causes memory fault
			if (Integer.parseInt(address, 2) > 15) {
				// illegal trap code
				MFR = "0010";
			}
			// goto trap code spot in the table
			int offset = Integer.parseInt(address, 2) % 16;
			MAR = Integer.parseInt(MBR, 2) + offset;
			MBR = memory.read(MAR);
			PC = MAR;
			// execute the routine
			processInstruction(memory, l1cache);
			// return to where you were
			MAR = 2;
			MBR = memory.read(MAR);
			PC = Integer.parseInt(MBR, 2);
			break;

		// FADD (27) - Floating add Memory to Register
		case "011011":
			// get the value of the register
			String value7 = FPR[GPR];
			// get the value of the memory
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			//if register has smaller (negative) exponent
			if(Integer.parseInt(value7.substring(1,2)) == 1 && Integer.parseInt(MBR.substring(1,2)) == 0) {
				//shift to get the same exponent
				int shiftBy = Integer.parseInt(value7.substring(2,8), 2) + Integer.parseInt(MBR.substring(2,8), 2);
				String smallVal = String
						.format("%16s",
								Integer.toBinaryString(Integer.parseInt(value7.substring(8), 2) >> shiftBy))
						.replace(" ", "0");
				int smallValInt;
				//determine new number and make positive or negative according to sign bit
				if(Integer.parseInt(value7.substring(0,1)) == 1) {
					smallValInt = -1 * Integer.parseInt(smallVal, 2);
				}
				else {
					smallValInt = Integer.parseInt(smallVal, 2);
				}
				//get the integer of the bigger exponent
				int bigValInt;
				if(Integer.parseInt(MBR.substring(0,1)) == 1) {
					bigValInt = -1 * Integer.parseInt(MBR.substring(8), 2);
				}
				else {
					bigValInt = Integer.parseInt(MBR.substring(8), 2);
				}
				//add the values together
				int addVal = bigValInt + smallValInt;
				//set sign bit accordingly
				String signBit = "0";
				if (addVal < 0) {
					addVal = addVal * -1;
					signBit = "1";
				}
				String addValString = Integer.toBinaryString(addVal);
				//determine if overflow
				if(addVal > 255) {
					addValString = addValString.substring(8);
						CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
								.replace(" ", "0");
				}
				//set the register with sign bit, original bigger exponent, and added values
				FPR[GPR] = signBit + MBR.substring(1,8) + addValString;
			}
			else if(Integer.parseInt(MBR.substring(1,2)) == 1 && Integer.parseInt(value7.substring(1,2)) == 0) {
				//shift to get the same exponent
				int shiftBy = Integer.parseInt(MBR.substring(2,8), 2) + Integer.parseInt(value7.substring(2,8), 2);
				String smallVal = String
						.format("%16s",
								Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
						.replace(" ", "0");
				int smallValInt;
				//determine new number and make positive or negative according to sign bit
				if(Integer.parseInt(MBR.substring(0,1)) == 1) {
					smallValInt = -1 * Integer.parseInt(smallVal, 2);
				}
				else {
					smallValInt = Integer.parseInt(smallVal, 2);
				}
				//get the integer of the bigger exponent
				int bigValInt;
				if(Integer.parseInt(value7.substring(0,1)) == 1) {
					bigValInt = -1 * Integer.parseInt(value7.substring(8), 2);
				}
				else {
					bigValInt = Integer.parseInt(value7.substring(8), 2);
				}
				//add the values together
				int addVal = bigValInt + smallValInt;
				//set sign bit accordingly
				String signBit = "0";
				if (addVal < 0) {
					addVal = addVal * -1;
					signBit = "1";
				}
				String addValString = Integer.toBinaryString(addVal);
				//determine if overflow
				if(addVal > 255) {
					addValString = addValString.substring(8);
						CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
								.replace(" ", "0");
				}
				//set the register with sign bit, original bigger exponent, and added values
				FPR[GPR] = signBit + value7.substring(1,8) + addValString;
			}else if(Integer.parseInt(MBR.substring(1,2)) == 1 && Integer.parseInt(value7.substring(1,2)) == 1){
				if(Integer.parseInt(value7.substring(2,8), 2) < Integer.parseInt(MBR.substring(2,8), 2)) {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(MBR.substring(3,8), 2) - Integer.parseInt(value7.substring(3,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(value7.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(value7.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(value7.substring(8), 2);
					}
					//add the values together
					int addVal = bigValInt + smallValInt;
					//set sign bit accordingly
					String signBit = "0";
					if (addVal < 0) {
						addVal = addVal * -1;
						signBit = "1";
					}
					String addValString = Integer.toBinaryString(addVal);
					//determine if overflow
					if(addVal > 255) {
						addValString = addValString.substring(8);
							CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
									.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + value7.substring(1,8) + addValString;
				}
				else {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(value7.substring(2,8), 2) - Integer.parseInt(MBR.substring(2,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(value7.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(MBR.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(MBR.substring(8), 2);
					}
					//add the values together
					int addVal = bigValInt + smallValInt;
					//set sign bit accordingly
					String signBit = "0";
					if (addVal < 0) {
						addVal = addVal * -1;
						signBit = "1";
					}
					String addValString = Integer.toBinaryString(addVal);
					//determine if overflow
					if(addVal > 255) {
						addValString = addValString.substring(8);
							CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
									.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + MBR.substring(1,8) + addValString;
				}
			} else {
				if(Integer.parseInt(value7.substring(2,8), 2) > Integer.parseInt(MBR.substring(2,8), 2)) {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(value7.substring(3,8), 2) - Integer.parseInt((MBR).substring(3,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(value7.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(value7.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(value7.substring(8), 2);
					}
					//add the values together
					int addVal = bigValInt + smallValInt;
					//set sign bit accordingly
					String signBit = "0";
					if (addVal < 0) {
						addVal = addVal * -1;
						signBit = "1";
					}
					String addValString = Integer.toBinaryString(addVal);
					//determine if overflow
					if(addVal > 255) {
						addValString = addValString.substring(8);
							CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
									.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + value7.substring(1,8) + addValString;
				} else {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(MBR.substring(2,8), 2) - Integer.parseInt(value7.substring(2,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(value7.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(value7.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(MBR.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(MBR.substring(8), 2);
					}
					//add the values together
					int addVal = bigValInt+ smallValInt;
					//set sign bit accordingly
					String signBit = "0";
					if (addVal < 0) {
						addVal = addVal * -1;
						signBit = "1";
					}
					String addValString = Integer.toBinaryString(addVal);
					//determine if overflow
					if(addVal > 255) {
						addValString = addValString.substring(8);
						CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0001", 2)))
									.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + MBR.substring(1,8) + addValString;
				}
			}
			break;
		// FSUB (28) - Floating Subtract Memory From Register
		case "011100":
			// get the value of the register
			String value8 = FPR[GPR];
			// get the value of the memory
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			//if register has smaller (negative) exponent
			if(Integer.parseInt(value8.substring(1,2)) == 1 && Integer.parseInt(MBR.substring(1,2)) == 0) {
				//shift to get the same exponent
				int shiftBy = Integer.parseInt(value8.substring(2,8), 2) + Integer.parseInt(MBR.substring(2,8), 2);
				String smallVal = String
						.format("%16s",
								Integer.toBinaryString(Integer.parseInt(value8.substring(8), 2) >> shiftBy))
						.replace(" ", "0");
				int smallValInt;
				//determine new number and make positive or negative according to sign bit
				if(Integer.parseInt(value8.substring(0,1)) == 1) {
					smallValInt = -1 * Integer.parseInt(smallVal, 2);
				}
				else {
					smallValInt = Integer.parseInt(smallVal, 2);
				}
				//get the integer of the bigger exponent
				int bigValInt;
				if(Integer.parseInt(MBR.substring(0,1)) == 1) {
					bigValInt = -1 * Integer.parseInt(MBR.substring(8), 2);
				}
				else {
					bigValInt = Integer.parseInt(MBR.substring(8), 2);
				}
				//add the values together
				int subVal = smallValInt - bigValInt;
				String subValString;
				//set sign bit accordingly
				String signBit = "0";
				if (subVal < 0) {
					subVal = subVal * -1;
					signBit = "1";
					subValString = Integer.toBinaryString(subVal);
				}
				else {
					subValString = String.format("%8s", Integer.toBinaryString(subVal)).replace(" ", "0");
				}
				//determine if overflow
				if(subVal < -256) {
					subValString = subValString.substring(8);
					CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
							.replace(" ", "0");
				}
				//set the register with sign bit, original bigger exponent, and added values
				FPR[GPR] = signBit + MBR.substring(1,8) + subValString;
			}
			else if(Integer.parseInt(MBR.substring(1,2)) == 1 && Integer.parseInt(value8.substring(1,2)) == 0) {
				//shift to get the same exponent
				int shiftBy = Integer.parseInt(MBR.substring(2,8), 2) + Integer.parseInt(value8.substring(2,8), 2);
				String smallVal = String
						.format("%16s",
								Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
						.replace(" ", "0");
				int smallValInt;
				//determine new number and make positive or negative according to sign bit
				if(Integer.parseInt(MBR.substring(0,1)) == 1) {
					smallValInt = -1 * Integer.parseInt(smallVal, 2);
				}
				else {
					smallValInt = Integer.parseInt(smallVal, 2);
				}
				//get the integer of the bigger exponent
				int bigValInt;
				if(Integer.parseInt(value8.substring(0,1)) == 1) {
					bigValInt = -1 * Integer.parseInt(value8.substring(8), 2);
				}
				else {
					bigValInt = Integer.parseInt(value8.substring(8), 2);
				}
				//add the values together
				int subVal = bigValInt - smallValInt;
				//set sign bit accordingly
				String signBit = "0";
				String subValString;
				if (subVal < 0) {
					subVal = subVal * -1;
					signBit = "1";
					subValString = Integer.toBinaryString(subVal);
				}
				else {
					subValString = String.format("%8s", Integer.toBinaryString(subVal)).replace(" ", "0");
				}
				//determine if overflow
				if(subVal > 255) {
					subValString = subValString.substring(8);
					CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
							.replace(" ", "0");
				}
				//set the register with sign bit, original bigger exponent, and added values
				FPR[GPR] = signBit + value8.substring(1,8) + subValString;
			}else if(Integer.parseInt(MBR.substring(1,2)) == 1 && Integer.parseInt(value8.substring(1,2)) == 1){
				if(Integer.parseInt(value8.substring(2,8), 2) < Integer.parseInt(MBR.substring(2,8), 2)) {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(MBR.substring(3,8), 2) - Integer.parseInt(value8.substring(3,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(value8.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(value8.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(value8.substring(8), 2);
					}
					//add the values together
					int subVal = bigValInt - smallValInt;
					//set sign bit accordingly
					String signBit = "0";
					String subValString;
					if (subVal < 0) {
						subVal = subVal * -1;
						signBit = "1";
						subValString = Integer.toBinaryString(subVal);
					}
					else {
						subValString = String.format("%8s", Integer.toBinaryString(subVal)).replace(" ", "0");
					}
					//determine if overflow
					if(subVal > 255) {
						subValString = subValString.substring(8);
						CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
								.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + value8.substring(1,8) + subValString;
				}
				else {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(value8.substring(2,8), 2) - Integer.parseInt(MBR.substring(2,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(value8.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(MBR.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(MBR.substring(8), 2);
					}
					//add the values together
					int subVal = smallValInt - bigValInt;
					//set sign bit accordingly
					String signBit = "0";
					String subValString;
					if (subVal < 0) {
						subVal = subVal * -1;
						signBit = "1";
						subValString = Integer.toBinaryString(subVal);
					}
					else {
						subValString = String.format("%8s", Integer.toBinaryString(subVal)).replace(" ", "0");
					}
					//determine if overflow
					if(subVal > 255) {
						subValString = subValString.substring(8);
						CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
								.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + MBR.substring(1,8) + subValString;
				}
			} else {
				if(Integer.parseInt(value8.substring(2,8), 2) > Integer.parseInt(MBR.substring(2,8), 2)) {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(value8.substring(3,8), 2) - Integer.parseInt((MBR).substring(3,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(MBR.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(value8.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(value8.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(value8.substring(8), 2);
					}
					//add the values together
					int subVal = bigValInt - smallValInt;
					//set sign bit accordingly
					String signBit = "0";
					String subValString;
					if (subVal < 0) {
						subVal = subVal * -1;
						signBit = "1";
						subValString = Integer.toBinaryString(subVal);
					}
					else {
						subValString = String.format("%8s", Integer.toBinaryString(subVal)).replace(" ", "0");
					}
					//determine if overflow
					if(subVal > 255) {
						subValString = subValString.substring(8);
						CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
								.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + value8.substring(1,8) + subValString;
				} else {
					//shift to get the same exponent
					int shiftBy = Integer.parseInt(MBR.substring(2,8), 2) - Integer.parseInt(value8.substring(2,8), 2);
					String smallVal = String
							.format("%16s",
									Integer.toBinaryString(Integer.parseInt(value8.substring(8), 2) >> shiftBy))
							.replace(" ", "0");
					int smallValInt;
					//determine new number and make positive or negative according to sign bit
					if(Integer.parseInt(value8.substring(0,1)) == 1) {
						smallValInt = -1 * Integer.parseInt(smallVal, 2);
					}
					else {
						smallValInt = Integer.parseInt(smallVal, 2);
					}
					//get the integer of the bigger exponent
					int bigValInt;
					if(Integer.parseInt(MBR.substring(0,1)) == 1) {
						bigValInt = -1 * Integer.parseInt(MBR.substring(8), 2);
					}
					else {
						bigValInt = Integer.parseInt(MBR.substring(8), 2);
					}
					//add the values together
					int subVal = smallValInt - bigValInt;
					//set sign bit accordingly
					String signBit = "0";
					String subValString;
					if (subVal < 0) {
						subVal = subVal * -1;
						signBit = "1";
						subValString = Integer.toBinaryString(subVal);
					}
					else {
						subValString = String.format("%8s", Integer.toBinaryString(subVal)).replace(" ", "0");
					}
					//determine if overflow
					if(subVal > 255) {
						subValString = subValString.substring(8);
						CC = String.format("%4s", Integer.toBinaryString(Integer.parseInt(CC, 2) | Integer.parseInt("0010", 2)))
								.replace(" ", "0");
					}
					//set the register with sign bit, original bigger exponent, and added values
					FPR[GPR] = signBit + MBR.substring(1,8) + subValString;
				}
			}
			break;
		
		// VADD (29) - Vector Add
		case "011101":
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			
			// Retrieve FR value, and FAULT if invalid	
			int frAdd = Integer.parseInt(FPR[GPR], 2);
			if (frAdd<1 || frAdd > 1023) {
				Main.faultOccured(2);
			}
			
			// Get the addresses of the two vectors in memory
			int addressAdd1 = MAR;
			int addressAdd2 = addressAdd1 + frAdd;
			
			// Iterate over the 2 vectors, adding them together
			// Then convert the result of each addition back to a string
			// Then write that result string back in address 1
			for (int i=0; i< frAdd; i++) {
				int sumInt = Integer.parseInt(l1cache.read(addressAdd1, memory), 2) + Integer.parseInt(l1cache.read(addressAdd2, memory), 2);
				String sumString = Integer.toString(sumInt, 2);
				while (sumString.length() < 12 ) {
					sumString = "0" + sumString;
				}
				l1cache.write(addressAdd1, sumString, memory);
				
				// Increment MAR and retrieve the next step of addresses from it
				MAR++;
				addressAdd1 = MAR;
				addressAdd2 = addressAdd1 + frAdd;
			}
			break;
			
		//VSUB (48) - Vector Subtract
		case "110000":
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			
			// Retrieve FR value, and FAULT if invalid		
			int frSub = Integer.parseInt(FPR[GPR], 2);
			if (frSub<1 || frSub > 1023) {
				Main.faultOccured(2);
			}
			
			// Get the addresses of the two vectors in memory
			int addressSub1 = MAR;
			int addressSub2 = addressSub1 + frSub;
			
			// Iterate over the 2 vectors, subtracting the second from the first
			// Then convert the result of each addition back to a string
			// Then write that result string back in address 1
			for (int i=0; i< frSub; i++) {
				int subInt = Integer.parseInt(l1cache.read(addressSub1, memory), 2) - Integer.parseInt(l1cache.read(addressSub2, memory), 2);
				String subString = Integer.toString(subInt, 2);
				while (subString.length() < 12 ) {
					subString = "0" + subString;
				}
				l1cache.write(addressSub1, subString, memory);
				
				// Increment MAR and retrieve the next step of addresses from it
				MAR++;
				addressSub1 = MAR;
				addressSub2 = addressSub1 + frSub;
			}
			break;
			
		// CNVRT (31) - Convert to fixed/floating point
		case "011111":
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			int F = Integer.parseInt(R[GPR], 2); // Get the value of F from the register
			
			// Place holders for the fixed point and floating point numbers
			String fixedPNumber = "";
			String floatingPNumber = "";
			
			if (F==0) { //then convert c(EA) to a fixed point number and store in r		
				// Get FP number from memory
				floatingPNumber = l1cache.read(MAR, memory);
				
				// Break down the FP number 
				int floatingPSign = Integer.parseInt(floatingPNumber.substring(0, 1), 2);
				int floatingPExponentSign = Integer.parseInt(floatingPNumber.substring(1, 2), 2);
				int floatingPExponent = Integer.parseInt(floatingPNumber.substring(2, 8), 2);
				String floatingPMantissa = floatingPNumber.substring(8,16);
				
				// Convert FP number to fixed point
				int i = floatingPExponent;
				fixedPNumber = floatingPMantissa;
				if ( floatingPExponentSign == 0 ) { // exponent is positive
					while (i > 0 && fixedPNumber.length() < 16) {
						fixedPNumber = fixedPNumber + "0";
						i--;
					}
				} else { // exponent is negative
					while (i > 0 && fixedPNumber.length() < 16) {
						fixedPNumber = "0" + fixedPNumber;
						i--;
					}
				}
				
				// Check if the FP number was negative, and if so, convert the new fixed point number to negative using two's complement
				String newFixedPNumber=""; // place holder for the converted fixed point number to negative
				if (floatingPSign==1) {
					for (int j=fixedPNumber.length(); j>0; j--) {
						char c = fixedPNumber.charAt(j);
						if (c == 0) { c='1';}
						else {c='0';}
						newFixedPNumber = newFixedPNumber + c;
					}
				} else {
					newFixedPNumber = fixedPNumber;
				}
				
				// Store fixed point number in the register
				R[GPR] = newFixedPNumber;
				
			} else if (F==1) { // then convert c(EA) to a floating point number and store in FR0
				// Get fixed point number from memory
				fixedPNumber = l1cache.read(MAR, memory);
				
				// Break down the fixed point number
				String integerPortion = fixedPNumber.substring(0,8);
				String fractionPortion = fixedPNumber.substring(8,16);
				
				// Convert fixed point to FP number
				// Create the mantissa
				// Find location of first '1' occurence in the integer portion and last '1' occurence in fraction portion
				int firstOccr = integerPortion.indexOf('1');
				int lastOccr = fractionPortion.lastIndexOf('1');
				String mantissaString = integerPortion.substring(firstOccr,8) + fractionPortion.substring(8,lastOccr);
				if (mantissaString.length() > 8 ) {
					mantissaString = mantissaString.substring(0,8);
				}
				// Create the exponent
				int exponentInt = 8 - firstOccr;
				String exponentString = Integer.toString(exponentInt, 2);
				if (exponentString.length() < 7 ) {
					while (exponentString.length() < 7 ) {
						exponentString = "0" + exponentString;
					}
				} else if ( exponentString.length() > 7) {
					exponentString = exponentString.substring(0,7);
				}
				
				// Compose the floating point number then save in FR0
				if (firstOccr > 0) { // this indicates that the exponent should be positive
					floatingPNumber = "0" + "0" + exponentString + mantissaString;
				} else { // this indicates that the exponent should be negative
					floatingPNumber = "0" + "1" + exponentString + mantissaString;
				}
				FPR[0] = floatingPNumber;
			}
			
			break;
			
			
		// LDFR (40) - Load Floating Register From Memory
		case "101000":
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = l1cache.read(MAR, memory);
			FPR[GPR] = MBR;
			break;
		
		//STFR (41) - Store Floating Register To Memory
		case "101001":
			MAR = calculateEffectiveAddress(memory, l1cache);
			MBR = FPR[GPR];
			l1cache.write(MAR, MBR, memory);
			break;
			
		// HALT (00) - Stop the program
		case "000000":
			console.haltConsole(true); // Halt the machine
			break;
			
		// If illegal op code
		default:
			Main.faultOccured(2);
			break;
		}
	}

	// Method to calculate the effective address from the address field that was in
	// IR
	// It has memory as a parameter because the memory object is not accessible from
	// this class
	public int calculateEffectiveAddress(Memory memory, L1Cache l1cache) {
		int EA;

		if (I == 0) {
			if (IX == 0) {
				EA = Integer.parseInt(address, 2);
			} else {
				EA = Integer.parseInt(address, 2) + Integer.parseInt(X[IX], 2);
			}
		} else {
			if (IX == 0) {
				MAR = Integer.parseInt(address, 2);
				EA = Integer.parseInt(l1cache.read(MAR, memory), 2);
			} else {
				MAR = Integer.parseInt(address, 2) + Integer.parseInt(X[IX], 2);
				EA = Integer.parseInt(l1cache.read(MAR, memory), 2);
			}
		}

		return EA;
	}
}