/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.cli;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * <p><code>Parser_8</code> creates {@link CommandLine}s.</p>
 *
 * @author John Keyes (john at integralsource.com)
 * @see Parser_8
 * @version $Revision: 551815 $
 */
public abstract class Parser_8 implements CommandLineParser {
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
    
    private class MissingArgumentRuntimeException extends RuntimeException {
    	private static final long serialVersionUID = 12313221321L;
    	MissingArgumentRuntimeException(String message) {
    		super(message);
    	}
    }
    
    private class UnrecognizedOptionRuntimeException extends RuntimeException {
    	private static final long serialVersionUID = 12313543521L;
    	UnrecognizedOptionRuntimeException(String message) {
    		super(message);
    	}
    }
    
    private class AlreadySelectedRuntimeException extends RuntimeException {
    	private static final long serialVersionUID = 12334543521L;
    	AlreadySelectedRuntimeException(String message) {
    		super(message);
    	}
    }
    
    private class BreakRuntimeException extends RuntimeException {
    	private static final long serialVersionUID = 22334543521L;
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

        // clear out the data in options in case it's been used before (CLI-71)        
        List list = options.helpOptions();
        list.forEach(x -> ((Option)x).clearValues());

        List requiredOptions = options.getRequiredOptions();
        CommandLine cmd = new CommandLine();

        if (arguments == null)
            arguments = new String[0];

        List tokenList = Arrays.asList(flatten(options, 
                                               arguments, 
                                               stopAtNonOption));

        ListIterator iterator = tokenList.listIterator();
        try {
	        iterator.forEachRemaining(x -> {
	        	String t = (String)x;
	
	        	boolean eatTheRest = false;
	            // the value is the double-dash
	            if ("--" == t)
	                eatTheRest = true;
	
	            // the value is a single dash
	            else if ("-" == t) {
	                if (stopAtNonOption)
	                    eatTheRest = true;
	                else
	                    cmd.addArg(t);
	            }
	            else if (t.startsWith("-"))
	            {
	                if (stopAtNonOption && !options.hasOption(t))
	                {
	                    eatTheRest = true;
	                    cmd.addArg(t);
	                }
	                else
	                {
	                    // if there is no option throw an UnrecognisedOptionException
	                    if (!options.hasOption(t))
	                        throw new UnrecognizedOptionRuntimeException("Unrecognized option: " + t);
	                    
	                    // get the option represented by arg
	                    final Option opt = options.getOption(t);
	
	                    // if the option is a required option remove the option from
	                    // the requiredOptions list
	                    if (opt.isRequired())
	                        requiredOptions.remove(opt.getKey());
	
	                    // if the option is in an OptionGroup make that option the selected
	                    // option of the group
	                    if (options.getOptionGroup(opt) != null)
	                    {
	                        OptionGroup group = options.getOptionGroup(opt);
	
	                        if (group.isRequired())
	                            requiredOptions.remove(group);
	
	                        try {
								group.setSelected(opt);
							} catch (AlreadySelectedException e) {
								throw new AlreadySelectedRuntimeException(e.getMessage());
							}
	                    }
	
	                    // if the option takes an argument value
	                    if (opt.hasArg())
	                    {
	                    	// loop until an option is found
	                    	iterator.forEachRemaining(y -> {
	                            String str = (String)y;
	
	                            // found an Option, not an argument
	                            if (options.hasOption(str) && str.startsWith("-"))
	                                iterator.previous();
	                            else
	                            	opt.addValueForProcessing( Util.stripLeadingAndTrailingQuotes(str) );
	                        });
	
	                        if ((opt.getValues() == null) && !opt.hasOptionalArg())
	                        	throw new MissingArgumentRuntimeException("Missing argument for option:" + opt.getKey());
	                    }
	
	
	                    // set the option on the command line
	                    cmd.addOption(opt);
	                }
	            }
	            else
	            {
	                cmd.addArg(t);
	
	                if (stopAtNonOption)
	                    eatTheRest = true;
	            }
	
	            // eat the remaining tokens
	            if (eatTheRest)
	            {
	            	iterator.forEachRemaining(z -> {
	            		String str = (String)z;
	            		
	                    // ensure only one double-dash is added
	                    if (!("--" == str))
	                        cmd.addArg(str);
	            	});
	            }
	        }); // end foreach loop
        } // end try
        catch (MissingArgumentRuntimeException e) {
        	throw new MissingArgumentException(e.getMessage());
        }
        catch (UnrecognizedOptionRuntimeException e) {
        	throw new UnrecognizedOptionException(e.getMessage());
        }
        catch (AlreadySelectedRuntimeException e) {
        	throw new AlreadySelectedException(e.getMessage());
        }
        
        // process each flattened token
        try {
	        if (properties != null) {
	        	Collections.list(properties.propertyNames()).forEach(x -> {
	        		String option = (String)x;
	        		
		            if (!cmd.hasOption(option))
		            {
		                Option opt = options.getOption(option);
		
		                // get the value from the properties instance
		                String value = properties.getProperty(option);
		
		                if (opt.hasArg()) {
		                    if (opt.getValues() == null || opt.getValues().length == 0)
	                            opt.addValueForProcessing(value);
		                }
		                else if (!value.toLowerCase().matches("yes|true|1")) 
		                	throw new BreakRuntimeException();
		                cmd.addOption(opt);
		            }
	        	});
	        }
        }
        catch (BreakRuntimeException e) {
        	// Nothing to do here
        }

        if (requiredOptions.size() > 0)
        {
        	String buff = requiredOptions.size() > 1 ? "Missing required options:" : "Missing required option:";
            // loop through the required options
        	buff += requiredOptions.stream().map(x -> x.toString()).collect(Collectors.joining(""));
        	
            throw new MissingOptionException(buff);
        }

        return cmd;
    }
}
