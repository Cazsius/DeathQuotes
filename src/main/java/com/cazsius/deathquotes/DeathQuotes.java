package com.cazsius.deathquotes;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStreamReader;

@Mod(DeathQuotes.ID)
public class DeathQuotes {

    private static final Logger LOGGER = LogManager.getLogger();
    public static String quotesFileName = "deathquotes.txt";
    public static String quotesPathAndFileName = "./config/"+quotesFileName;
    public static String[] quotes = null;

    public static final String ID = "deathquotes";


    public static MinecraftServer server = null; // needed by a function in Do.
    public static boolean isWindowsOS = System.getProperty("os.name").contains("Windows");
    public static String theNewLine = "\n";

    public DeathQuotes() {

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

    }

    private void setup(final FMLCommonSetupEvent event) {

        if ( isWindowsOS ) { theNewLine = "\r\n"; }

        // if no quotes file in config folder create the default from the one in the jar file assets folder
        if (! Do.fileExists(quotesPathAndFileName) )
        { StringBuilder output = new StringBuilder();
            BufferedReader readBuffer = null;
            String aLine = "";
            boolean addWithNewline = false;
            boolean readingLines = true;
            try { readBuffer = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("assets/"+quotesFileName), "UTF-8")); } // open the input stream
            catch (UnsupportedEncodingException e1) { readingLines = false; Do.Err("The file "+ quotesFileName + " seems to be missing in the jar file and the config folder."); }
            while ( readingLines ) {
                // aLine = readBuffer.readLine(); // THIS SCREWS UP reading single quotes in the file and changes them to a question mark!! in mc1.9 and up! so bs!
                aLine = readTextLine(readBuffer); // read the next line
                if ( aLine != null )
                {	if (! aLine.matches("^\\s*$") ) { // if it is not a blank line
                    if (addWithNewline) { output.append(theNewLine); } else { addWithNewline = true; } // prevents a blank line at end of the output file
                    output.append(aLine);
                }
                } else { readingLines = false; } // null means nothing more to read
            } // end while read loop
            if ( output.length() > 0 ) { Do.StringToFile(quotesPathAndFileName, output.toString()); }
            output = new StringBuilder();
            try { readBuffer.close(); } catch (IOException e1) {}
        } // end create the default quotes file

        // load the quotes file into an array for use
        quotes = Do.FileToString(quotesPathAndFileName).split("\n");
        for(int i=0; i<quotes.length; i++) { quotes[i]=quotes[i].trim(); }

        // Status Ready
        System.out.println("["+DeathQuotes.ID+"] Death quotes count "+ quotes.length + " from file "+ quotesPathAndFileName );
    }

    /**Returns the text from a line from the BufferedReader or null if both nothing read and end of file has been reached.
     * For compatibility with Windows and *nix text file formats this reads \n or \r\n from the buffer but does not return them with the text.
     * This also has a custom and badly coded but functional fix for nonstandard quotes used in the text file.
     * @param bufferedReader
     * @return String or null
     */
    public String readTextLine(BufferedReader bufferedReader) {
        boolean endOfFile = false;
        boolean didReadSomething = false;
        boolean readingChars = true;
        StringBuilder outputString = new StringBuilder();
        int aCharInt = -1;
        while ( readingChars ) {
            try {
                aCharInt = bufferedReader.read(); // totally messes up the quote marks with 65533
                //if ( aCharInt > 127 ) { Do.Trace("aCharInt > 127 : "+ aCharInt +"  =================================="); }
                if ( aCharInt == 65533 ) { aCharInt = 39; } // a quick and improper fix
                if (aCharInt != -1) { didReadSomething = true; }
            } catch (IOException e)      { endOfFile=true; readingChars = false; }
            if  ( aCharInt == -1 )       { endOfFile=true; readingChars = false; }
            if (( aCharInt == 10 ) || ( aCharInt == 13)) { readingChars = false; } //  \n or \r
            //if (( aCharInt == 8216 ) || ( aCharInt == 8217 )) { aCharInt = 39; } // custom code for compatibility
            if (( aCharInt != -1 ) && ( aCharInt != 10) && ( aCharInt != 13 )) { outputString.append((char) aCharInt); }
        }

        // Compatibility with \r\n (0D0A hex) from windows text editors:
        if ( aCharInt == 13 ) {
            try { bufferedReader.mark(1); } catch (IOException e1) {}
            aCharInt = 10;
            try { aCharInt = bufferedReader.read(); } catch (IOException e) {}
            if ( aCharInt != 10 ) { try { bufferedReader.reset(); } catch (IOException e) {} }
        }

        if (( endOfFile ) && ( ! didReadSomething )) { return null; }
        return outputString.toString();
    }

    @SubscribeEvent
    public void serverStarting(FMLServerStartingEvent event) {
        server=event.getServer(); // needed by a function in Do.
    }
}
