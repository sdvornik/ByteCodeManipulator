package com.yahoo.sdvornik;



import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSigner;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Stream;

import static java.lang.System.out;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class JarReader {

  private final String pathToInputJar;

  private final String pathToOutputDirectory;

  private final Configuration conf;

  public JarReader(String pathToInputJar, String pathToOutputDirectory, Configuration conf) {

    this.pathToInputJar = pathToInputJar;

    this.pathToOutputDirectory = pathToOutputDirectory;

    this.conf = conf;
  }

  public void extract() throws Exception {

    String inputJarName = Paths.get(pathToInputJar).getFileName().toString();

    String outputJarName = inputJarName.replace(".jar", "-mod.jar");

    String pathToOutputJar = Paths.get(pathToOutputDirectory).resolve(outputJarName).toString();

    JarFile jar = new JarFile(this.pathToInputJar);

    Manifest manifestOld = jar.getManifest();


    Manifest manifestNew = new Manifest();
    Attributes attrs = manifestNew.getMainAttributes();
    manifestOld.getMainAttributes().entrySet().forEach(entry -> {
      attrs.put(entry.getKey(), entry.getValue());
    });

    JarOutputStream jos = new JarOutputStream(new FileOutputStream(pathToOutputJar), manifestNew);

    Enumeration jarEntries = jar.entries();

    while (jarEntries.hasMoreElements()) {

      JarEntry inputJarEntry = (JarEntry) jarEntries.nextElement();

      InputStream is = jar.getInputStream(inputJarEntry);

      String jarEntryName = inputJarEntry.getName();

      // skip manifest
      if (jarEntryName.toLowerCase().startsWith("meta-inf/manifest")) continue;

      boolean modified = false;
      JavaClass jcMod = null;
      // process class
      if (jarEntryName.endsWith(".class")) {
        String clazz = jarEntryName.replace(".class", "").replace("/", ".");

        ClassParser parser = new ClassParser(is, clazz);

        JavaClass jc = parser.parse();

        ClassGen modClass = null;

        Method[] methodArr = null;

        for (String elm : jc.getInterfaceNames()) {
          switch (elm) {
            case "java.sql.PreparedStatement":
              modClass = new ClassGen(jc);
              methodArr = jc.getMethods();
              for (Method m : methodArr) {
                switch (m.getName()) {

                  // "executeQuery", "executeUpdate", "execute", "addBatch"

                }
              }

            break;

            case "java.sql.CallableStatement":
              modClass = new ClassGen(jc);
              methodArr = jc.getMethods();
              for (Method m : methodArr) {
                switch (m.getName()) {

                  // "executeQuery", "executeUpdate", "execute", "addBatch"

                }
              }
            break;

            case "java.sql.Connection":
              modClass = new ClassGen(jc);
              methodArr = jc.getMethods();
              for (Method m : methodArr) {
                switch (m.getName()) {

                  // "prepareStatement", "prepareCall", "nativeSQL"

                }
              }

              break;

            case "java.sql.Statement":
              modClass = new ClassGen(jc);
              methodArr = jc.getMethods();
              for (Method m : methodArr) {
                switch (m.getName()) {

                  // "executeQuery", "executeUpdate", "execute", "addBatch"

                }
              }

              break;

            case "java.sql.DatabaseMetaData":

              modClass = new ClassGen(jc);

              methodArr = jc.getMethods();

              for (Method m : methodArr) {
                switch (m.getName()) {
                  case "getDatabaseMajorVersion":
                    if (conf.databaseMajorVersion != null) {
                      modified = true;
                      createGetDatabaseMajorVersion(modClass, m);
                    }
                    break;

                  case "getDatabaseProductVersion":
                    if (conf.databaseProductVersion != null) {
                      modified = true;
                      createGetDatabaseProductVersion(modClass, m);
                    }
                    break;
                  case "getDatabaseProductName":
                    if (conf.databaseProductName != null) {
                      modified = true;
                      createGetDatabaseProductName(modClass, m);
                    }
                    break;
                }
              }

              if (modified) {
                out.println("modified");
                jcMod = modClass.getJavaClass();
              }

              break;
          }
        }
      }
      jos.putNextEntry(new JarEntry(inputJarEntry.getName()));

      if (!modified) {
        is = jar.getInputStream(inputJarEntry);
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
          jos.write(buffer, 0, bytesRead);
        }
        is.close();
      }
      else {
        byte[] buffer = jcMod.getBytes();
        jos.write(buffer, 0, buffer.length);
      }
      jos.flush();
      jos.closeEntry();
    }
    // add Transformator in jar
    if(conf.matchers != null && conf.replacers != null) {

      JavaClass jcMod = modifyTransformator();
      jos.putNextEntry(new JarEntry(jcMod.getClassName()));
      byte[] buffer = jcMod.getBytes();
      jos.write(buffer, 0, buffer.length);
      jos.flush();
      jos.closeEntry();
    }
    jos.close();
  }

  public JavaClass modifyTransformator() {
    try {
      JavaClass jc = Repository.lookupClass("com.yahoo.sdvornik.Transformator");
      ClassGen modClass = new ClassGen(jc);

      Method[] methodArr = modClass.getMethods();
      for(int i = 0; i < methodArr.length; ++i) {
        if(! methodArr[i].getName().equals("<clinit>")) continue;
        Method oldMethod = methodArr[i];
        out.println(methodArr[i].getName());

        int refToStrArr = modClass.getConstantPool().lookupClass("java.util.regex.Pattern");
        out.println(refToStrArr);
          //.addArrayClass(new ArrayType(Type.STRING, 1));

        InstructionList instructionList = new InstructionList();
        instructionList.append(new PUSH(modClass.getConstantPool(), conf.replacers.length));
        instructionList.append(new ANEWARRAY(refToStrArr));
        for(int j = 0; j < conf.replacers.length; ++j) {
          instructionList.append(new DUP());
          instructionList.append(new PUSH(modClass.getConstantPool(), j));
          instructionList.append(new LDC(modClass.getConstantPool().addString(conf.replacers[j])));
          instructionList.append(new AASTORE());
        }
        instructionList.append(
          new PUTSTATIC(
            modClass.getConstantPool()
              .lookupFieldref(
                modClass.getClassName(),
                "replacers",
                "[Ljava/lang/String;"// String signature
              )
          )
        );

        out.println(modClass.getConstantPool()
          .lookupFieldref(
            modClass.getClassName(),
            "patterns",
            "[Ljava/util/regex/Pattern;"// String signature
          ));

        int refToMatchersArr = modClass.getConstantPool().addArrayClass(new ArrayType(
          new ObjectType("java.util.regex.Pattern"), 1)
        );

        out.println(modClass.getConstantPool());
        instructionList.append(new PUSH(modClass.getConstantPool(), conf.matchers.length));
        instructionList.append(new ANEWARRAY(refToMatchersArr));
        /*
        for(int j = 0; j < conf.matchers.length; ++j) {
          instructionList.append(new DUP());
          instructionList.append(new PUSH(modClass.getConstantPool(), j));
          instructionList.append(new LDC(modClass.getConstantPool().addString(conf.matchers[j])));
          instructionList.append(new AASTORE());
        }

        instructionList.append(
          new PUTSTATIC(
            modClass.getConstantPool()
              .lookupFieldref(
                modClass.getClassName(),
                "matchers",
                "[Ljava/util/regex/Pattern;"// String signature
              )
          )
        );
*/

        /*
    ICONST_2
    ANEWARRAY java/lang/String
    DUP
    ICONST_0
    LDC "c"
    AASTORE
    DUP
    ICONST_1
    LDC "d"
    AASTORE
    PUTSTATIC com/yahoo/sdvornik/Transformator.replacers : [Ljava/lang/String;
         */


        MethodGen mg = new MethodGen(
          Constants.ACC_PUBLIC,
          oldMethod.getReturnType(),
          oldMethod.getArgumentTypes(),
          null,
          oldMethod.getName(),
          modClass.getClassName(),
          instructionList,
          modClass.getConstantPool()
        );
        mg.setMaxLocals();
        mg.setMaxStack();

        modClass.removeMethod(oldMethod);
        modClass.addMethod(mg.getMethod());

      }


      /*
      ConstantPoolGen cpGen = modClass.getConstantPool();
      int size = modClass.getFields().length;

      FieldGen strField = new FieldGen(
        Constants.ACC_PUBLIC|Constants.ACC_STATIC,
        new ArrayType(Type.STRING, 1),
        "",
        cpGen
      );

      for(int i = 0; i< size; ++i) {
        Field f = modClass.getFields()[i];

        out.println(modClass.getFields()[i]+"   "+i);
      }
      */
      return modClass.getJavaClass();
    }
    catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  public void createGetDatabaseMajorVersion(ClassGen modClass, Method oldMethod) {

    InstructionList instructionList = new InstructionList();

    instructionList.append(new LDC(modClass.getConstantPool().addInteger(conf.databaseMajorVersion)));
    instructionList.append(new IRETURN());

    MethodGen mg = new MethodGen(
      Constants.ACC_PUBLIC,
      Type.INT,
      oldMethod.getArgumentTypes(),
      null,
      oldMethod.getName(),
      modClass.getClassName(),
      instructionList,
      modClass.getConstantPool()
    );
    mg.setMaxLocals();
    mg.setMaxStack();

    modClass.removeMethod(oldMethod);
    modClass.addMethod(mg.getMethod());
  }

  public void createGetDatabaseProductVersion(ClassGen modClass, Method oldMethod) {

    InstructionList instructionList = new InstructionList();

    instructionList.append(new LDC(modClass.getConstantPool().addString(conf.databaseProductVersion)));
    instructionList.append(new ARETURN());

    MethodGen mg = new MethodGen(
      Constants.ACC_PUBLIC,
      Type.STRING,
      oldMethod.getArgumentTypes(),
      null,
      oldMethod.getName(),
      modClass.getClassName(),
      instructionList,
      modClass.getConstantPool()
    );
    mg.setMaxLocals();
    mg.setMaxStack();

    modClass.removeMethod(oldMethod);
    modClass.addMethod(mg.getMethod());
  }


  public Method createGetDatabaseProductName(JavaClass mod, Method method, ConstantPoolGen cp) {
    MethodGen mg = new MethodGen(method, mod.getClassName(), cp);

    InstructionList ils = new InstructionList();

    ils.append(new LDC(cp.addString(conf.databaseProductName)));
    ils.append(new ARETURN());

    mg.setMaxLocals();
    mg.setMaxStack();

    return mg.getMethod();
  }

  public void createGetDatabaseProductName(ClassGen modClass, Method oldMethod) {

    InstructionList instructionList = new InstructionList();

    instructionList.append(new LDC(modClass.getConstantPool().addString(conf.databaseProductName)));
    instructionList.append(new ARETURN());

    MethodGen mg = new MethodGen(
      Constants.ACC_PUBLIC,
      Type.STRING,
      oldMethod.getArgumentTypes(),
      null,
      oldMethod.getName(),
      modClass.getClassName(),
      instructionList,
      modClass.getConstantPool()
    );
    mg.setMaxLocals();
    mg.setMaxStack();

    modClass.removeMethod(oldMethod);
    modClass.addMethod(mg.getMethod());
  }

}
