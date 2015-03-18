/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package edu.psu.cse.siis.ic3;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Main;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.AnalyzeJimpleClass;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.StringResource;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.entryPointCreators.AndroidEntryPointCreator;
import soot.options.Options;

public class SetupApplication {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Map<String, Set<AndroidMethod>> callbackMethods =
      new HashMap<String, Set<AndroidMethod>>(10000);

  private Set<String> entrypoints = null;

  private String appPackageName = "";

  private final String apkFileLocation;
  private final String classDirectory;
  private final String androidClassPath;
  private ProcessManifest manifest;

  public SetupApplication(String apkFileLocation, String classDirectory, String androidClassPath) {
    this.apkFileLocation = apkFileLocation;
    this.classDirectory = classDirectory;
    this.androidClassPath = androidClassPath;
  }

  public ProcessManifest getManifest() {
    return this.manifest;
  }

  /**
   * Prints list of classes containing entry points to stdout
   */
  public void printEntrypoints() {
    if (logger.isDebugEnabled()) {
      if (this.entrypoints == null) {
        logger.debug("Entry points not initialized");
      } else {
        logger.debug("Classes containing entry points:");
        for (String className : entrypoints) {
          logger.debug("\t" + className);
        }
        logger.debug("End of Entrypoints");
      }
    }
  }

  /**
   * Calculates the sets of sources, modifiers, entry points, and callbacks methods for the given
   * APK file.
   * 
   * @param sourceMethods The set of methods to be considered as sources
   * @param modifierMethods The set of methods to be considered as modifiers
   * @throws IOException Thrown if the given source/modifier file could not be read.
   */
  public Map<String, Set<String>> calculateSourcesSinksEntrypoints(
      Set<AndroidMethod> sourceMethods, Set<AndroidMethod> modifierMethods,
      ProcessManifest processMan) throws IOException {
    this.manifest = processMan;

    if (this.manifest == null) {
      this.manifest = new ProcessManifest();
      this.manifest.loadManifestFile(apkFileLocation);
    }

    // To look for callbacks, we need to start somewhere. We use the Android
    // lifecycle methods for this purpose.
    this.appPackageName = this.manifest.getPackageName();
    this.entrypoints = this.manifest.getEntryPointClasses();

    boolean parseLayoutFile = !apkFileLocation.endsWith(".xml");

    // Parse the resource file
    ARSCFileParser resParser = null;
    if (parseLayoutFile) {
      resParser = new ARSCFileParser();
      resParser.parse(apkFileLocation);
    }

    AnalyzeJimpleClass jimpleClass = null;
    LayoutFileParser lfp =
        parseLayoutFile ? new LayoutFileParser(this.appPackageName, resParser) : null;

    boolean hasChanged = true;
    while (hasChanged) {
      hasChanged = false;
      soot.G.reset();
      initializeSoot();

      if (jimpleClass == null) {
        // Collect the callback interfaces implemented in the app's source code
        jimpleClass = new AnalyzeJimpleClass(entrypoints);
        jimpleClass.collectCallbackMethods();

        // Find the user-defined sources in the layout XML files. This
        // only needs to be done once, but is a Soot phase.
        if (parseLayoutFile) {
          lfp.parseLayoutFile(apkFileLocation, entrypoints);
        }
      } else {
        jimpleClass.collectCallbackMethodsIncremental();
      }

      // Run the soot-based operations
      PackManager.v().runPacks();

      // Collect the results of the soot-based phases
      for (Entry<String, Set<AndroidMethod>> entry : jimpleClass.getCallbackMethods().entrySet()) {
        if (this.callbackMethods.containsKey(entry.getKey())) {
          if (this.callbackMethods.get(entry.getKey()).addAll(entry.getValue())) {
            hasChanged = true;
          }
        } else {
          this.callbackMethods.put(entry.getKey(), new HashSet<AndroidMethod>(entry.getValue()));
          hasChanged = true;
        }
      }
    }

    // Collect the XML-based callback methods
    for (Entry<SootClass, Set<Integer>> lcentry : jimpleClass.getLayoutClasses().entrySet()) {
      for (Integer classId : lcentry.getValue()) {
        AbstractResource resource = resParser.findResource(classId);
        if (resource instanceof StringResource) {
          StringResource strRes = (StringResource) resource;
          if (lfp.getCallbackMethods().containsKey(strRes.getValue())) {
            for (String methodName : lfp.getCallbackMethods().get(strRes.getValue())) {
              Set<AndroidMethod> methods = this.callbackMethods.get(lcentry.getKey().getName());
              if (methods == null) {
                methods = new HashSet<AndroidMethod>();
                this.callbackMethods.put(lcentry.getKey().getName(), methods);
              }

              // The callback may be declared directly in the class
              // or in one of the superclasses
              SootMethod callbackMethod = null;
              SootClass callbackClass = lcentry.getKey();
              while (callbackMethod == null) {
                if (callbackClass.declaresMethodByName(methodName)) {
                  String subSig = "void " + methodName + "(android.view.View)";
                  for (SootMethod sm : callbackClass.getMethods()) {
                    if (sm.getSubSignature().equals(subSig)) {
                      callbackMethod = sm;
                      break;
                    }
                  }
                }
                if (callbackClass.hasSuperclass()) {
                  callbackClass = callbackClass.getSuperclass();
                } else {
                  break;
                }
              }
              if (callbackMethod == null) {
                if (logger.isWarnEnabled()) {
                  logger.warn("Callback method " + methodName + " not found in class "
                      + lcentry.getKey().getName());
                }
                continue;
              }
              methods.add(new AndroidMethod(callbackMethod));
            }
          }
        } else if (logger.isWarnEnabled()) {
          logger.warn("Unexpected resource type for layout class");
        }
      }
    }

    logger.info("Entry point calculation done.");

    // Clean up everything we no longer need
    soot.G.reset();

    Map<String, Set<String>> result = new HashMap<>(this.callbackMethods.size());
    for (Map.Entry<String, Set<AndroidMethod>> entry : this.callbackMethods.entrySet()) {
      Set<AndroidMethod> callbackSet = entry.getValue();
      Set<String> callbackStrings = new HashSet<>(callbackSet.size());

      for (AndroidMethod androidMethod : callbackSet) {
        callbackStrings.add(androidMethod.getSignature());
      }

      result.put(entry.getKey(), callbackStrings);
    }

    return result;
  }

  /**
   * Initializes soot for running the soot-based phases of the application metadata analysis
   * 
   * @return The entry point used for running soot
   */
  public SootMethod initializeSoot() {
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_output_format(Options.output_format_none);
    Options.v().set_whole_program(true);
    // Options.v().setPhaseOption("cg.spark", "geom-pta:true");
    // Options.v().setPhaseOption("cg.spark", "geom-encoding:PtIns");
    // Options.v().set_ignore_resolution_errors(true);
    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v()
        .set_soot_classpath(this.classDirectory + File.pathSeparator + this.androidClassPath);
    if (logger.isDebugEnabled()) {
      logger.debug("Android class path: " + this.androidClassPath);
    }
    // Options.v().set_android_jars(androidJar);
    // Options.v().set_src_prec(Options.src_prec_apk);
    Options.v().set_process_dir(new ArrayList<>(this.entrypoints));
    Options.v().set_app(true);
    Main.v().autoSetOptions();

    Scene.v().loadNecessaryClasses();

    for (String className : this.entrypoints) {
      SootClass c = Scene.v().forceResolve(className, SootClass.BODIES);
      c.setApplicationClass();
    }

    SootMethod entryPoint = getEntryPointCreator().createDummyMain();
    Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
    return entryPoint;
  }

  public AndroidEntryPointCreator getEntryPointCreator() {
    AndroidEntryPointCreator entryPointCreator =
        new AndroidEntryPointCreator(new ArrayList<String>(this.entrypoints));
    Map<String, List<String>> callbackMethodSigs = new HashMap<String, List<String>>();
    for (String className : this.callbackMethods.keySet()) {
      List<String> methodSigs = new ArrayList<String>();
      callbackMethodSigs.put(className, methodSigs);
      for (AndroidMethod am : this.callbackMethods.get(className)) {
        methodSigs.add(am.getSignature());
      }
    }
    entryPointCreator.setCallbackFunctions(callbackMethodSigs);
    return entryPointCreator;
  }
}
