package components;

/* The ROM class contains the code (TRAP, HALT, etc.) as a place holder for the first 6 reserved memory locations,
 * as well as the ROM loader function which 1- deposits that code into memory, and then 2- sets the memory starting 
 * address and initial Program Counter values to be used by the computer.
 */
public class ROM {
	public String[] lines = new String[7];

	public ROM() {
		lines[0] = "0111100000000000";
		lines[1] = "0000000000000110"; // Address 6 which is the location of FAULT routine
		lines[2] = "0000000000000000"; // Location for storing PC from TRAP
		lines[3] = "0000000000000000"; // Not used
		lines[4] = "0000000000000000"; // Location for storing PC from FAULT
		lines[5] = "0000000000000000"; // Not used
		lines[6] = "0111100000000000"; // Code for TRAP instruction
	}

	// Function called by the IPL button to load the content of ROM into memory
	public void ROMloader(components.Memory memory, components.CPU cpu, consoles.Console console,
			consoles.TechnicianConsole technicianConsole, components.L1Cache l1cache) {

		// Clear content of memory and CPU registers
		cpu.clearCPURegisters();
		memory.initializeMemory();

		// Clear the output and memory displays
		console.clearOutputField();
		technicianConsole.clearRAMField();
		l1cache.clearCache();

		// Load memory with the content of ROM
		for (int i = 0; i < lines.length; i++) {
			memory.addr[i] = lines[i];
		}

		// Set the starting address in memory which is where the Program Counter would
		// point to
		memory.STARTINGADDRESS = 8;
		cpu.PC = memory.STARTINGADDRESS;
	}
}
