package components;

import java.util.LinkedList;

/* The L1Cache class simulates a level 1 cache for the processor, a fully associative unified cache.
 * It is 16 lines long, each with 4 word blocks. Follows the FIFO approach for replacing cache lines.
 * Interaction with this class is through the read and write functions
 */

public class L1Cache {
	// Set cache constants
	public int MAXNUMBEROFLINES = 16;
	public int WORDSPERLINE = 4;
	public class CACHELINE {
		public String tag; //10 bits used to identify the line (ex: "1000000000")
		public String[] word = new String[WORDSPERLINE]; //4 words per cache line, each cache line words is 16 bit long
		
		// CACHELINE constructor
		CACHELINE(String tag, String[] word) {
			this.tag = tag;
			this.word = word;
		}
	}
	
	// Instantiate a linked list of type CACHELINE
	public LinkedList<CACHELINE> cache = new LinkedList<CACHELINE>();
	
	// Function to write to the cache at a given memory address. Takes address and data as parameters
	// and also the memory object, in order to be able to write to memory when the blocked to be replaced has the dirty bit set
	public void write(int decimalAddress, String data, Memory memory) {
		
		// Check if address is referencing a reserved memory location, or is greater than maximum memory size, and if so, HALT
		if (decimalAddress == 0 || decimalAddress == 1 || decimalAddress == 3 || decimalAddress == 5  ) {
			Main.faultOccured(0);
			return;
		} else if (decimalAddress >= 2048 ) {
			Main.faultOccured(0);
			return;
		}
		
		// Convert address to a 12 bit binary string then split into a tag and an offset
		String address = convertAddressToString(decimalAddress);
		String[] parsedAddress = parseAddress(address);
		String tag_from_address = parsedAddress[0];
		int offset_from_address = Integer.parseInt(parsedAddress[1], 2);
		
		// Use this variable later to store the result of the check for the address in cache
		boolean addressAlreadyInCache = false;
		
		// Iterate over every cache line to check if the address is already there (in order to update its content with the new data)
		for (int i=0; i < cache.size(); i++) {
			if (cache.get(i).tag.compareTo(tag_from_address) == 0) { // Check if any cache line holds the address we're looking for
				if (cache.get(i).word[offset_from_address].substring(0, 1).compareTo("1") == 0) { //If the word's dirty bit is set then first commit that word's content to memory
					String wordToCommit = cache.get(i).word[offset_from_address].substring(1, 17);
					commitToMemory(cache.get(i).tag, offset_from_address, wordToCommit, memory);
				}
				
				// Then write to that address and set the dirty bit	to "1"
				CACHELINE lineToUpdate = cache.get(i);
				lineToUpdate.word[offset_from_address] = "1" + data;
				cache.set(i, lineToUpdate);
				// memory.write(decimalAddress,data); // Comment or un-comment to write directly to memory or not, used for testing
				
				addressAlreadyInCache = true; // Set variable to true so that the code in the following "if" statement doesn't run
				break; // Exit the "for" loop since we found the address
			} 
		}
		
		// If address not found in cache, then find a cache line to use 
		if (addressAlreadyInCache == false) { 
			if (cache.size() == MAXNUMBEROFLINES) {
				CACHELINE lineToRemove = cache.removeFirst(); // Remove the first cache line
				for (int i=0; i<WORDSPERLINE; i++) { // Check if any of the words in that line have a dirty bit and if so commit them to memory
					if (lineToRemove.word[i].substring(0, 1).compareTo("1") == 0) { 
						String wordToCommit = lineToRemove.word[i].substring(1, 17);
						commitToMemory(lineToRemove.tag, i, wordToCommit, memory);
					}
				}	
			}
			
			// Compose a new cache line containing the data, and then add it to the cache
			String[] wordsToAdd = new String[]{"xxxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxxxxx"};
			wordsToAdd[offset_from_address] = "1" + data;
			CACHELINE lineToAdd = new CACHELINE(tag_from_address, wordsToAdd);
			cache.add(lineToAdd);
		}
	}
	
	// Function to read from the cache and either return the content from the cache (when found) or from memory (when not found in cache)
	// Takes the address and memory object as input and returns the requested content 
	public String read(int decimalAddress, Memory memory) {
		
		// Check if address is greater than maximum memory size, and if so, go to fault routine
		if (decimalAddress >= 2048 ) {
			Main.faultOccured(3);
			return "0000000000000000";
		} 
		
		// Check if address is the first 6 reserved addresses and if return directly from memory
		if (decimalAddress < 6 ) {
			String content = memory.read(decimalAddress);
			return content;
		}
		
		// Convert address to a 12 bit binary string then split into a tag and an offset
		String address = convertAddressToString(decimalAddress);
		String[] parsedAddress = parseAddress(address);
		String tag_from_address = parsedAddress[0];
		int offset_from_address = Integer.parseInt(parsedAddress[1], 2);
		
		// Iterate over every cache line checking if the address is in the cache, and if so return its content excluding the dirty bit
		for (int i=0; i<cache.size(); i++ ) {
			if (cache.get(i).tag.compareTo(tag_from_address) == 0) {
				String wordInCache = cache.get(i).word[offset_from_address];
				if (wordInCache.length() == 17 && wordInCache.compareTo("xxxxxxxxxxxxxxxxx") != 0) {
					return cache.get(i).word[offset_from_address].substring(1, 17);
				} else {
					break;
				}
			}
		}
		
		// If address is not found in cache, then get the content from memory, cache it, then return it
		String content = memory.read(decimalAddress);
		write(decimalAddress, content, memory);
		return content;
	}
	
	// Helper function to convert a decimal address to a string address
	public String convertAddressToString(int decimalAddress) {
		String address = Integer.toString(decimalAddress, 2);
		while (address.length() < 12 ) {
			address = "0" + address;
		}
		return address;
	}
	
	// Helper function to commit the old content of the word into memory
	public void commitToMemory(String tag, int offset_from_address, String wordToCommit, Memory memory) {
		// Generate the memory address to write to
		String offset = Integer.toString(offset_from_address, 2); // convert offset from integer to string in order to merge it with the tag
		while (offset.length() < 2 ) { offset = "0" + offset; } // ensure the offset is 2 bits long	
		String targetAddressString = tag + offset; // merge tag and offset to form the memory address 
		int targetAddress = Integer.parseInt(targetAddressString, 2);
		
		// Write to memory
		memory.write(targetAddress, wordToCommit);
	}
	
	// Helper function to split the memory address into a tag and an offset
	public String[] parseAddress(String address) {
		String[] parsedAddress = new String[2];
		parsedAddress[0] = address.substring(0, 10);
		parsedAddress[1] = address.substring(10, 12);
		return parsedAddress;
	}
	
	// Helper function used to return a string of all the words in a given cache line number (to display in the cache output printer) 
	public String getWordsFromLine(int linenumber) {
		return cache.get(linenumber).tag + "\t" + cache.get(linenumber).word[0] + "\t" + cache.get(linenumber).word[1] + "\t" + cache.get(linenumber).word[2] + "\t" + cache.get(linenumber).word[3];
	}
	
	// Function to clear the cache (used by the IPL button)
	public void clearCache() {
		for (int i=0; i<cache.size(); i++ ) {
			cache.removeFirst();
		}
	}
}
