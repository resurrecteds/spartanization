package org.apache.commons.cli;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

/**
 * <p><code>Parser_7</code> creates {@link CommandLine}s.</p>
 *
 * @author John Keyes (john at integralsource.com)
 * @see Parser_7
 * @version $Revision: 551815 $
 */
public abstract class Parser_7 implements CommandLineParser{

    /** commandline instance */
    private CommandLine cmd;

    /** current Options */
    private Options options;

    /** list of required options strings */
    private List requiredOptions;

    /**
     * <p>Subclasses must implement this method to reduce
     * the <code>arguments</code> that have been passed to the parse 
     * method.</p>
     *
     * @param opts The Options to parse the arguments by.
     * @param arguments The arguments that have to be flattened.
     * @param stopAtNonOption specifies whether to stop 
     * flattening when a non option has been encountered
     * @return a String array of the flattened arguments
     */
    protected abstract String[] flatten(Options opts, String[] arguments, 
                                        boolean stopAtNonOption);

    /**
     * <p>Parses the specified <code>arguments</code> 
     * based on the specifed {@link Options}.</p>
     *
     * @param options the <code>Options</code>
     * @param arguments the <code>arguments</code>
     * @return the <code>CommandLine</code>
     * @throws ParseException if an error occurs when parsing the
     * arguments.
     */
    public CommandLine parse(Options options, String[] arguments)
                      throws ParseException
    {
        return parse(options, arguments, null, false);
    }

    /**
     * Parse the arguments according to the specified options and
     * properties.
     *
     * @param options the specified Options
     * @param arguments the command line arguments
     * @param properties command line option name-value pairs
     * @return the list of atomic option and value tokens
     *
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     */
    public CommandLine parse(Options options, String[] arguments, 
                             Properties properties)
        throws ParseException
    {
        return parse(options, arguments, properties, false);
    }

    /**
     * <p>Parses the specified <code>arguments</code> 
     * based on the specifed {@link Options}.</p>
     *
     * @param options the <code>Options</code>
     * @param arguments the <code>arguments</code>
     * @param stopAtNonOption specifies whether to stop 
     * interpreting the arguments when a non option has 
     * been encountered and to add them to the CommandLines
     * args list.
     *
     * @return the <code>CommandLine</code>
     * @throws ParseException if an error occurs when parsing the
     * arguments.
     */
    public CommandLine parse(Options options, String[] arguments, 
                             boolean stopAtNonOption)
        throws ParseException
    {
        return parse(options, arguments, null, stopAtNonOption);
    }

    /**
     * Parse the arguments according to the specified options and
     * properties.
     *
     * @param options the specified Options
     * @param arguments the command line arguments
     * @param properties command line option name-value pairs
     * @param stopAtNonOption stop parsing the arguments when the first
     * non option is encountered.
     *
     * @return the list of atomic option and value tokens
     *
     * @throws ParseException if there are any problems encountered
     * while parsing the command line tokens.
     */
    public CommandLine parse(Options options, String[] arguments, 
                             Properties properties, boolean stopAtNonOption)
        throws ParseException
    {
        // initialise members
        this.options = options;
        // clear out the data in options in case it's been used before (CLI-71)
        for (Iterator it = options.helpOptions().iterator(); it.hasNext();) {
            Option opt = (Option) it.next();
            opt.clearValues();
        }

        cmd = new CommandLine();

        if (arguments == null) {
            arguments = new String[0];
        }

        List tokenList = Arrays.asList(flatten(this.options, 
                                               arguments, 
                                               stopAtNonOption));

        ListIterator iterator = tokenList.listIterator();
        // process each flattened token  
        while (iterator.hasNext()) {
            String t = (String) iterator.next();

            // the value is the double-dash
            if ("--".equals(t)) {
                break;
            }

            // the value is a single dash
            if ("-".equals(t)) {
                if (stopAtNonOption) {
                    break;
                }
                cmd.addArg(t);
                continue;
            }
            
            // the value is an option
            if (t.startsWith("-")) {
                if (stopAtNonOption && !options.hasOption(t)) {
                    cmd.addArg(t);
                    break;
                }
                processOption(t, iterator);
                continue;
            }

            // the value is an argument
            cmd.addArg(t);

            if (stopAtNonOption){
                break;
            }
        }
        
        // eat the remaining tokens
        while (iterator.hasNext()) {
            String str = (String) iterator.next();

            // ensure only one double-dash is added
            if (!"--".equals(str)) {
                cmd.addArg(str);
            }
        }

        // TODO - remove this method too
        processProperties(properties);
        
        requiredOptions = options.getRequiredOptions();
        if (requiredOptions.size() > 0) {
            StringBuffer buff = new StringBuffer("Missing required option:");
            if (requiredOptions.size() > 1) {
            	buff = new StringBuffer("Missing required options:");
            }
            
            Iterator iter = requiredOptions.iterator();
            // loop through the required options
            while (iter.hasNext()) {
                buff.append(iter.next());
            }

            throw new MissingOptionException(buff.toString());
        }

        return cmd;
    }

    /**
     * <p>Sets the values of Options using the values in 
     * <code>properties</code>.</p>
     *
     * @param properties The value properties to be processed.
     */
    private void processProperties(Properties properties)
    {
        if (properties == null) {
            return;
        }

        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String option = e.nextElement().toString();

            if (!cmd.hasOption(option)) {
                Option opt = options.getOption(option);

                // get the value from the properties instance
                String value = properties.getProperty(option);

                if (opt.hasArg()) {
                    if ((opt.getValues() == null)
                        || (opt.getValues().length == 0)) {
                        try {
                            opt.addValueForProcessing(value);
                        }
                        catch (RuntimeException exp) {
                            // if we cannot add the value don't worry about it
                        }
                    }
                }
                else if (!("yes".equalsIgnoreCase(value) 
                           || "true".equalsIgnoreCase(value)
                           || "1".equalsIgnoreCase(value))) {
                    // if the value is not yes, true or 1 then don't add the
                    // option to the CommandLine
                    break;
                }

                cmd.addOption(opt);
            }
        }
    }

    /**
     * <p>Throws a {@link MissingOptionException} if all of the
     * required options are no present.</p>
     *
     * @throws MissingOptionException if any of the required Options
     * are not present.
     */
//    private void checkRequiredOptions()
//        throws MissingOptionException {
//        // if there are required options that have not been
//        // processsed
//        if (requiredOptions.size() > 0) {
//            Iterator iter = requiredOptions.iterator();
//            StringBuffer buff = new StringBuffer("Missing required option");
//            buff.append(requiredOptions.size() == 1 ? "" : "s");
//            buff.append(": ");
//
//            // loop through the required options
//            while (iter.hasNext()) {
//                buff.append(iter.next());
//            }
//
//            throw new MissingOptionException(buff.toString());
//        }
//    }

    /**
     * <p>Process the argument values for the specified Option
     * <code>opt</code> using the values retrieved from the 
     * specified iterator <code>iter</code>.
     *
     * @param opt The current Option
     * @param iter The iterator over the flattened command line
     * Options.
     *
     * @throws ParseException if an argument value is required
     * and it is has not been found.
     */
//    private void processArgs(Option opt, ListIterator iter)
//        throws ParseException {
//        // loop until an option is found
//        while (iter.hasNext()) {
//            String str = (String) iter.next();
//
//            // found an Option, not an argument
//            if (options.hasOption(str) && str.startsWith("-")) {
//                iter.previous();
//                break;
//            }
//
//            // found a value
//            try {
//                opt.addValueForProcessing( Util.stripLeadingAndTrailingQuotes(str) );
//            }
//            catch (RuntimeException exp) {
//                iter.previous();
//                break;
//            }
//        }
//
//        if ((opt.getValues() == null) && !opt.hasOptionalArg()) {
//            throw new MissingArgumentException("Missing argument for option:"
//                                               + opt.getKey());
//        }
//    }

    /**
     * <p>Process the Option specified by <code>arg</code>
     * using the values retrieved from the specfied iterator
     * <code>iter</code>.
     *
     * @param arg The String value representing an Option
     * @param iter The iterator over the flattened command 
     * line arguments.
     *
     * @throws ParseException if <code>arg</code> does not
     * represent an Option
     */
    private void processOption(String arg, ListIterator iter)
        throws ParseException {
        boolean hasOption = options.hasOption(arg);

        // if there is no option throw an UnrecognisedOptionException
        if (!hasOption) {
            throw new UnrecognizedOptionException("Unrecognized option: " 
                                                  + arg);
        }
        
        // get the option represented by arg
        final Option opt = options.getOption(arg);

        // if the option is a required option remove the option from
        // the requiredOptions list
        if (opt.isRequired()) {
            requiredOptions.remove(opt.getKey());
        }

        // if the option is in an OptionGroup make that option the selected
        // option of the group
        OptionGroup group = options.getOptionGroup(opt);
        if (group != null) {
            if (group.isRequired()) {
                requiredOptions.remove(group);
            }

            group.setSelected(opt);
        }

        // if the option takes an argument value
        if (opt.hasArg()) {
//            processArgs(opt, iter);
        	while (iter.hasNext()) {
                String str = (String) iter.next();

                // found an Option, not an argument
                if (options.hasOption(str) && str.startsWith("-")) {
                    iter.previous();
                    break;
                }

                // found a value
                try {
                    opt.addValueForProcessing( Util.stripLeadingAndTrailingQuotes(str) );
                }
                catch (RuntimeException exp) {
                    iter.previous();
                    break;
                }
            }

            if ((opt.getValues() == null) && !opt.hasOptionalArg()) {
                throw new MissingArgumentException("Missing argument for option:"
                                                   + opt.getKey());
            }
        }


        // set the option on the command line
        cmd.addOption(opt);
    }

}
