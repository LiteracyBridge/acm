package org.literacybridge.acm.utils;

public class CommandLineArguments {
	public String repositoryDirectoryPath;
	public String databaseDirectoryPath;

	private CommandLineArguments() {}
	
	public static CommandLineArguments analyseCommandLineArguments(String[] arguments) {
		CommandLineArguments args = new CommandLineArguments();
		
		if (arguments.length != 2) {
			System.out.println("To override config.properties, add argument with db path followed by argument for repository path.");		
		} else {
			args = new CommandLineArguments();
			args.databaseDirectoryPath = arguments[0];
			args.repositoryDirectoryPath = arguments[1];
			
			System.out.println(args);
		}
		
		return args;
 	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Path to Database = " + databaseDirectoryPath).append("\n");
		sb.append("Path to Repository (Content) = " + repositoryDirectoryPath).append("\n");
		
		return "CommandLineArguments: \n" + sb.toString();
	}
}
