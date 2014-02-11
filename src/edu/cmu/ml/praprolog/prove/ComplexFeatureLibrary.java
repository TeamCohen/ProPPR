package edu.cmu.ml.praprolog.prove;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Malcolm Greaves
 */
public class ComplexFeatureLibrary {

    static public final String ESCAPE_PREFIX = "escape__";
    //
    static private boolean initizliaed = false;
    static private Map<String, ComplexFeature> functor2cf;
    static private LogicProgram lp;

    static public boolean isComplexFeature(Goal g) {
        if (g == null)
            throw new NullPointerException("goal cannot be null");
        return isComplexFeature(g.getFunctor());
    }

    static public boolean isComplexFeature(String functor) {
        if (functor == null)
            throw new NullPointerException("functor cannot be null");
        return functor.startsWith(ESCAPE_PREFIX);
    }

    /**
     * @param functor
     * @return the ComplexFeature for the registered functor, null if functor is not registered
     * @throws IllegalArgumentException if not iniitalized
     * @throws NullPointerException     if functor is null
     * @throws IllegalArgumentException if functor is zero-length
     */
    static public ComplexFeature getFeature(String functor) {
        if (!initizliaed)
            throw new IllegalStateException("ComplexFeatureLibrary is not initialized");
        if (functor == null)
            throw new NullPointerException("functor cannot be null");
        if (functor.length() == 0)
            throw new IllegalArgumentException("functor cannot be zero length");
        return functor2cf.get(functor);
    }

    static public void init(LogicProgram logProg, BufferedReader r)
            throws IOException, ClassNotFoundException, ClassCastException,
            InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (r == null)
            throw new NullPointerException("reader cannot be null");

        if (logProg == null)
            throw new NullPointerException("logic program cannot be null");
        lp = logProg;

        functor2cf = new HashMap<String, ComplexFeature>();
        // load & construct functor classes
        for (String line; (line = r.readLine()) != null; ) {
            line = line.trim();
            if (line.charAt(0) != '#') {
                // lines that start with # are comments
                final String[] bits = line.split("=");
                if (bits.length != 2)
                    throw new IllegalArgumentException("improper format, need %s=%s[,%s]*\\n, not \"" +
                                                       line + "\"");

                final String functor = bits[0].trim();
                if (functor.length() == 0)
                    throw new IllegalArgumentException("cannot have zero-length functor (line: \"" +
                                                       line + "\"");

                bits[1] = bits[1].trim();
                if (bits[1].length() == 0)
                    throw new IllegalArgumentException("cannot have zero-length ComplexFeature class & args string (line: \"" +
                                                       line + "\"");
                final int firstComma = bits[1].indexOf(",");
                final String[] constructionArgs;
                final String classStr;
                if (firstComma == -1) {
                    classStr = bits[1];
                    constructionArgs = new String[0];
                } else {
                    classStr = bits[1].substring(0, firstComma);
                    constructionArgs = bits[1].substring(firstComma + 1).split(",");
                }
                if (classStr.length() == 0)
                    throw new IllegalArgumentException("cannot have zero length ComplexFeature class (line: \"" +
                                                       line + "\"");

                final Class c;
                try {
                    c = Class.forName(classStr);
                } catch (ClassNotFoundException e) {
                    throw new ClassNotFoundException("Couldn't find class for functor: \"" +
                                                     functor + "\" line: \"" + line + "\"\n", e);
                }

                final ComplexFeature cf;
                try {
                    String[] x = new String[0];
                    cf = (ComplexFeature) c.getDeclaredConstructor(LogicProgram.class, String[].class)
                                           .newInstance(lp, (Object) constructionArgs);

                } catch (InstantiationException e) {
                    throw new InstantiationException("Couldn't construct for functor: \"" +
                                                     functor + "\" line: \"" + line + "\"\n" + e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new IllegalAccessException("Couldn't access constructor for functor: \"" +
                                                     functor + "\" line: \"" + line + "\"\n" + e.getMessage());
                } catch (ClassCastException e) {
                    throw new ClassCastException("functor class doesn't extend ComplexFeature: \"" +
                                                 classStr + "\" line: \"" + line + "\"\n" + e.getMessage());
                } catch (NoSuchMethodException e) {
                    throw new NoSuchMethodException("ComplexFeature class \"" + c +
                                                    "\" doesn't have String[] constructor line: \"" +
                                                    line + "\"\n" + e.getMessage());
                } catch (InvocationTargetException e) {
                    throw new NoSuchMethodException("ComplexFeature class \"" + c +
                                                    "\" cannot call with String[] constructor line: \"" +
                                                    line + "\"\n" + e.getMessage());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("ComplexFeature class \"" + c +
                                                       "\" line: \"" + line + "\"\n" + e.getMessage());
                }

                functor2cf.put(functor, cf);
                if (functor.startsWith(ESCAPE_PREFIX)) {
                    functor2cf.put(new String(functor.split(ESCAPE_PREFIX)[1]), cf);
                } else {
                    functor2cf.put(ESCAPE_PREFIX + functor, cf);
                }
            }
        }

        initizliaed = true;
    }

    /**
     * Destroys current state (if any) and loads the functor -> ComplexFeature mapping
     * from the properties file.
     *
     * @param propsFile
     * @throws IOException            for reading through properties file
     * @throws ClassNotFoundException if a given functor's class is not present
     * @lp logic program
     */

    static public void init(LogicProgram logProg, String propsFile)
            throws IOException, ClassNotFoundException, ClassCastException,
            InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (propsFile == null)
            throw new NullPointerException("properties file cannot be null");
        File propF = new File(propsFile);
        if (!propF.isFile())
            throw new IllegalArgumentException("properties file doesn't exist: " + propF);

        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(propsFile));
            init(logProg, r);
        } finally {
            if (r != null) r.close();
        }
    }
}
