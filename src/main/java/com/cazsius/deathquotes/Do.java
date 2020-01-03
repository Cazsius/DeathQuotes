package com.cazsius.deathquotes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andre L Noel
 * @version 2016-11-20
 *
 */

public class Do {

    public Do() {

    }

    // General Minecraft Modding Programming Tools

    /**Returns a string of all the data in the specified file. It is intended to conveniently load any text file into a string.
     * @param filePathAndName
     * @return String
     */
    public static String FileToString( String filePathAndName ) {
        Reader istream;
        try {
            istream = new FileReader(filePathAndName);
        } catch (FileNotFoundException e) {
            Err("FILE NOT FOUND for file " + filePathAndName);
            e.printStackTrace();
            return "";
        }
        StringBuilder sbuffer = new StringBuilder( );
        char[] b = new char[128];
        int n;
        try {
            while ((n = istream.read(b)) > 0) { sbuffer.append(b, 0, n); }
        } catch (IOException e) {
            Err("Could not read from file " + filePathAndName);
            e.printStackTrace();
        }
        try {
            istream.close();
        } catch (IOException e) {
            Err("Could not close the read stream for file " + filePathAndName);
            e.printStackTrace();
        }
        return sbuffer.toString();
    }

    /**Returns true if it successfully writes the file to storage with the string data.
     * @param filePathAndName
     * @param filedata
     * @return boolean true if successful
     */
    public static boolean StringToFile( String filePathAndName, String filedata ) {
        Pattern pattern = Pattern.compile("^(.*)/");
        Matcher matcher = pattern.matcher(filePathAndName);
        if ( matcher.find() ) { folderMake( matcher.group(1) ); } // create folder path if it doesn't exist
        File fh = new File(filePathAndName);
        if ( ! fh.exists() ) {
            try {
                fh.createNewFile();
            } catch (IOException e) {
                Err("Could not create file " + filePathAndName );
                e.printStackTrace();
                return false;
            }
        }
        OutputStream ostream;
        try {
            ostream = new FileOutputStream(filePathAndName);
        } catch (FileNotFoundException e1) {
            Err("Could not open file output stream for " + filePathAndName);
            e1.printStackTrace();
            return false;
        }
        // put filedata to the ostream here
        byte[] data = filedata.getBytes( );
        try {
            ostream.write(data, 0, data.length);
        } catch (IOException e1) {
            Err("Could not write data to file " + filePathAndName);
            e1.printStackTrace();
            try {
                ostream.close();
            } catch (IOException e) {
                Err("Could not close output stream for file " + filePathAndName + " after the above write error.");
                e.printStackTrace();
            }
            return false;
        }
        try {
            ostream.close();
        } catch (IOException e) {
            Err("Could not close output stream for file " + filePathAndName);
            e.printStackTrace();
            return false;
        }
        return true;
    }

  /**
     * Outputs a string with mod name and "TRACE" in the console.
     * @param s
     */
    public static void Trace(String s) {
        System.out.println("TRACE [" + DeathQuotes.ID + "] " + s);
    }

    /**
     * Displays text message in chat to the player, if parameter player is not null.
     * @param player
     * @param theMessage
     */
    public static void Say(PlayerEntity player, String theMessage) {
        if (player != null) {
            player.sendMessage(new StringTextComponent(theMessage)); // mc1.11
            //player.addChatComponentMessage(new TextComponentString(theMessage)); // mc1.9 to mc1.10.2
            //player.addChatComponentMessage(new ChatComponentText(theMessage)); // mc1.7.10 and mc1.7.2 and mc1.8
            //player.addChatMessage(theMessage); // mc1.6.4
        } else { Do.Trace("Do.Say: player is null, message is:"+theMessage);
        }
    }


  /**
     * Sends the mod name and a string message to the standard error output channel and to player chat if the player is not null.
     * @param player
     * @param s
     */
    public static void Err(PlayerEntity player, String s) {
        Say(player,"ERROR [" + DeathQuotes.ID + "] " + s);
        Err(s);
    }

    /**
     * Sends the mod name and a string message to the standard error output channel.
     * @param s
     */
    public static void Err(String s) {
        System.err.println("ERROR [" + DeathQuotes.ID + "] " + s);
    }

  /**Returns true if it exists and it is a data file, not a folder.
     * @param filePathAndName
     * @return boolean
     */
    public static boolean fileExists(String filePathAndName) {
        File fh = new File(filePathAndName);
      return fh.exists() && !fh.isDirectory();
    }

  /**
     * Attempts to create all folders specified in path, if they do not exist.
     * @param filePath
     */
    public static boolean folderMake (String filePath) {
        File fh = new File(filePath);
        return fh.mkdirs();
    }

    // Configuration Array system: String[n][2] contains variable names and values.  All are string.  Names are case insensitive and may include spaces between other displayable characters.
    // In the future this should not use arrays.  This should use something like a dynamic list.


}
