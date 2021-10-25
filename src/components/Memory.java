package components;

import java.util.Arrays;

/* The Memory class is a representation of the computer's memory, as an array of fixed size, 
 * and contains the functions needed to READ or WRITE from or to memory.
 */
public class Memory {
	
	public int SIZE = 2048; //Contains the size of memory (i.e. array) - Constant value
	public int STARTINGADDRESS; //Contains the starting address of where instructions or data can start getting loaded - Constant value
	
	public String[] addr = new String[SIZE]; //Variable simulating memory
	
	//Class constructor
	public Memory() {
		initializeMemory();
	}
	
	//Function used when the program needs to read from memory
	public String read(int MAR) {
		// Check if address is referencing a reserved memory location, or is greater than maximum memory size, and if so, HALT
		if (MAR >= SIZE) {
			Main.faultOccured(3);
		}
		
		return this.addr[MAR];
	}
	
	//Function used when the program needs to write from memory
	public void write(int address, String value) {
		// Check if address is referencing a reserved memory location, or is greater than maximum memory size, and if so, HALT
		if (address == 0 || address == 1 || address == 3 || address == 5  ) {
			Main.faultOccured(0);
		} else if (address >= (SIZE) ) {
			Main.faultOccured(3);
		}
				
		this.addr[address] = value;
	}
	
	//Function called to reset the memory (such as by the memory constructor or IPL buttons)
	public void initializeMemory()	{
		Arrays.fill(addr, "0000000000000000");
	}
	
	// Getter for the size instance variable
	public int getSize() {
		return this.SIZE;
	}	
}
