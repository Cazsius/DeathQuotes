package com.cazsius.deathquotes.utils;

import com.cazsius.deathquotes.config.Settings;
import com.cazsius.deathquotes.impl.LimitedSet;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Stream;

import static com.cazsius.deathquotes.utils.Constants.*;

public final class Funcs {
    private static String[] quotes = null;
    private static LimitedSet<Integer> quotesSet;
    private static final Random randomGenerator = new Random();
    private static State state = State.IDLE;

    public static State getState() {
        return state;
    }

    public static boolean copyQuotesToConfig() {
        Path sourceDirectory;
        Optional<Path> optionalPath = getQuotesFileDirFromJar();
        if (!optionalPath.isPresent()) {
            return false;
        }
        sourceDirectory = optionalPath.get();
        Path targetDirectory = Paths.get(quotesPathAndFileName);
        try {
            Files.copy(sourceDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.error("Couldn't copy the file \"" + quotesFileName + "\" from jar to \"config\" folder!");
            return false;
        }
        return true;
    }

    public static boolean quotesFileExistsInConfig() {
        return fileExists(quotesPathAndFileName);
    }

    public static boolean configDirExists() {
        return folderExists(quotesConfigPath);
    }

    public static boolean fileExists(String filePathAndName) {
        File fh = new File(filePathAndName);
        return fh.exists() && !fh.isDirectory();
    }

    public static boolean folderExists(String folderPath) {
        File fh = new File(folderPath);
        return fh.exists() && fh.isDirectory();
    }

    public static boolean createConfigDir() {
        try {
            Files.createDirectories(Paths.get(quotesConfigPath));
            return true;
        } catch (Exception ex) {
            Logger.error("Couldn't create \"config\" folder in root directory!");
            return false;
        }
    }

    public static Optional<Path> getQuotesFileDirFromJar() {
        try {
            URI uri = Objects.requireNonNull(Funcs.class.getClassLoader().getResource("assets/" + quotesFileName)).toURI();
            return Optional.of(Paths.get(uri));
        } catch (Exception ex) {
            Logger.error("Couldn't find the file \"" + quotesFileName + "\" in jar!");
            return Optional.empty();
        }
    }

    public static boolean isBlank(String string) {
        return string.isEmpty() || string.chars().boxed().allMatch(Character::isWhitespace);
    }

    public static boolean loadQuotes(boolean fromJar) {
        State previousState = state;
        state = State.LOADING_QUOTES;
        Path sourceDirectory;
        boolean encodingException = true;
        if (fromJar) {
            Optional<Path> optionalPath = getQuotesFileDirFromJar();
            if (!optionalPath.isPresent()) {
                state = previousState;
                return false;
            }
            sourceDirectory = optionalPath.get();
        } else {
            sourceDirectory = Paths.get(quotesPathAndFileName);
        }
        List<Charset> charsets = new ArrayList<>();
        charsets.add(StandardCharsets.UTF_8);
        charsets.add(StandardCharsets.US_ASCII);
        charsets.add(StandardCharsets.UTF_16);
        for (Charset charset : charsets) {
            try (Stream<String> lines = Files.lines(sourceDirectory, charset)) {
                quotes = lines.filter(s -> !isBlank(s)).map(String::trim).toArray(String[]::new);
                int percent = Settings.getNonRepeatablePercent();
                int quotesNumber;
                switch (percent) {
                    case 0: {
                        quotesNumber = 0;
                        break;
                    }
                    case 100: {
                        quotesNumber = quotes.length;
                        break;
                    }
                    default: {
                        quotesNumber = (int) Math.ceil((double) quotes.length / 100 * percent);
                        break;
                    }
                }
                if (quotesNumber >= quotes.length) {
                    quotesNumber = quotes.length - 1;
                }
                quotesSet = new LimitedSet<>(quotesNumber, Settings.getClearListOfNonRepeatableQuotes());
            } catch (UncheckedIOException ex) {
                continue;
            } catch (IOException ex) {
                encodingException = false;
                break;
            }
            state = previousState;
            // Status - Ready
            Logger.info("Loaded death quotes!");
            Logger.info("Death quotes count - " + Funcs.getQuotesLength());
            return true;
        }
        state = previousState;
        Logger.error("Couldn't read quotes the file \"" + quotesFileName + "\" from " +
                     (fromJar ? "jar" : "\"config\" folder") + (encodingException ? " because encoding wasn't \"UTF-8\"" : "") + "!");
        Logger.error("Death quotes won't work because there is no quotes available!");
        Logger.error("You can delete the file " + quotesFileName + " and restart Minecraft for default quotes! " +
                     "Or edit that file and reload it in the game with command \"/deathquotes reloadQuotes\"!");
        return false;
    }

    public static int getQuotesLength() {
        return (quotes == null) ? 0 : quotes.length;
    }

    public static String getRandomQuote() {
        if (quotesSet.getSize() > 0) {
            final int maxIterations = quotesSet.getSize() * 2;
            for (int i = 0; i < maxIterations; i++) {
                int randomNumber = randomGenerator.nextInt(Funcs.getQuotesLength());
                if (quotesSet.contains(randomNumber)) continue;
                quotesSet.add(randomNumber);
                return quotes[randomNumber];
            }
            Logger.error(String.format("Searched for the fresh quote for too long (more than %s tries)!", maxIterations));
        }
        return quotes[randomGenerator.nextInt(Funcs.getQuotesLength())];
    }

    public static String handleQuote(String quote, String playerName) {
        // Replace player name string if needed
        String replaceString = Settings.getPlayerNameReplaceString();
        if (!isBlank(replaceString) && quote.contains(replaceString)) {
            quote = quote.replace(replaceString, playerName);
        }
        // Replace next line string if needed
        replaceString = Settings.getNextLineReplaceString();
        if (!isBlank(replaceString) && quote.contains(replaceString)) {
            quote = quote.replace(replaceString, "\n");
            if (Settings.getEnableTrimmingBeforeAndAfterNextLine()) {
                quote = quote.replaceAll("[ \t]*\n[ \t]*", "\n");
            }
        }
        // Add quotation marks if needed
        if (Settings.getEnableQuotationMarks()) {
            quote = MessageFormat.format("\"{0}\"", quote);
        }
        return quote;
    }
}
