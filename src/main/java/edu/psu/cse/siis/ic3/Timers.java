/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Ondrej Lhotak
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package edu.psu.cse.siis.ic3;

import java.text.DecimalFormat;

import soot.G;
import soot.Timer;
import soot.options.Options;

public class Timers {
  private static Timers instance = new Timers();

  private Timers() {
  }

  public static Timers v() {
    synchronized (instance) {
      return instance;
    }
  }

  public static void clear() {
    instance = new Timers();
  }

  public Timer modelParsing = new Timer("modelParsing");

  public Timer mainGeneration = new Timer("mainGeneration");

  public Timer misc = new Timer("Misc");

  public Timer classLoading = new Timer("Class loading");

  public Timer totalTimer = new Timer("totalTimer");

  public Timer entryPointMapping = new Timer("entryPointMapping");

  public int entryPoints = 0;

  public int reachableMethods = 0;

  public int classes = 0;

  public void printProfilingInformation() {
    long totalTime = totalTimer.getTime();

    G.v().out.println("Time measurements");
    G.v().out.println();

    G.v().out.println("    Main generation: " + toTimeString(mainGeneration, totalTime));

    G.v().out.println();

    G.v().out.println("    Entry points: " + entryPoints);
    G.v().out.println("     Class count: " + classes);

    // Print out time stats.
    G.v().out.println("totalTime:" + toTimeString(totalTimer, totalTime));

    if (Options.v().subtract_gc()) {
      G.v().out.println("Garbage collection was subtracted from these numbers.");
      G.v().out.println("       forcedGC:"
          + toTimeString(G.v().Timer_forcedGarbageCollectionTimer, totalTime));
    }
  }

  private String toTimeString(Timer timer, long totalTime) {
    DecimalFormat format = new DecimalFormat("00.0");
    DecimalFormat percFormat = new DecimalFormat("00.0");

    long time = timer.getTime();

    String timeString = format.format(time / 1000.0);

    return timeString + "s (" + percFormat.format(time * 100.0 / totalTime) + "%)";
  }
}
