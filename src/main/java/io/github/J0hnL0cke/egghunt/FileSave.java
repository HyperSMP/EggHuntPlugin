package io.github.J0hnL0cke.egghunt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public final class FileSave {
	private Path rel_dir=Paths.get("/EggHunt/data");
	String file_ext=".txt";
	
	//Saves data in key-value pairs, saving is abstracted into private methods
	
	//TODO: this is a temporary implement mongodb saving when able
	//TODO: make saving async
	
	public void writeKey(String key, String value) {
		if (!keyExists(key)) {
			createFile(key.concat(file_ext));
		}
		try {
			Files.writeString(rel_dir, value);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getKey(String key, String not_found) {
		if (keyExists(key)) {
			try {
				return Files.readAllLines(rel_dir).get(0);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		else {
			return not_found;
		}
	}
	
	public boolean keyExists(String key) {
		checkPathAccessable();
		return Files.exists(rel_dir.resolve(key));
		
	}
	
	//private methods
	
	private void checkPathAccessable() {
		//makes sure the path in rel_loc exists, creates it if not, and checks if it is readable and writable
		if (Files.exists(rel_dir)) {
			//if files are not readable or not writable, warn the user
			if (!Files.isReadable(rel_dir) || !Files.isWritable(rel_dir)) {
				EggHuntListener.logger.warning(String.format("Warning: Path \"%s\": readable: %b, writable: %b.",rel_dir,Files.isReadable(rel_dir),Files.isWritable(rel_dir)));
			}
		}
		else {
			EggHuntListener.logger.info(String.format("Path \"%s\" does not exist, creating.",rel_dir));
			try {
				Files.createDirectories(rel_dir);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void createFile(String filename) {
		try {
			Files.createFile(rel_dir.resolve(filename));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
