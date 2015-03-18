package edu.psu.cse.siis.ic3;


public class Main {
  public static void main(String[] args) {
    Ic3Analysis analysis = new Ic3Analysis();
    Ic3CommandLineParser parser = new Ic3CommandLineParser();
    Ic3CommandLineArguments commandLineArguments =
        parser.parseCommandLine(args, Ic3CommandLineArguments.class);
    if (commandLineArguments == null) {
      return;
    }
    commandLineArguments.processCommandLineArguments();
    analysis.performAnalysis(commandLineArguments);
  }
}
