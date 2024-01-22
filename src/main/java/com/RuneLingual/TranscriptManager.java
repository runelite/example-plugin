package com.RuneLingual;

import java.text.Normalizer;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscriptManager implements Serializable
{
    private Map<String, Map<String, String>> transcript = new HashMap<>();
    
    private LogHandler logger;
    private static final long serialVersionUID = 9190132562154153951L;

    public void addTranscript(String name, String text) throws Exception
    {
        /*** Only useful to master db
         *
         * if found transcript does not exist within the database
         * adds a new entry to it
         *
         * a git diff-based system could be implemented to commit updates later on***/
        String nameKey = sanitizeString(name, false);
        String textKey = sanitizeString(text, false);

        // manually ignores lines from the ge prices plugin
        if(textKey.startsWith("colnormalprice"))
        {
            logger.log("Ignoring line from item prices plugin.");
            return;  // ignores ge prices plugin strings
        }

        Map<String, String> npcTranscripts = transcript.get(nameKey);
        if (npcTranscripts == null)
        {
            npcTranscripts = new HashMap<>();
            npcTranscripts.put("name", name);
            transcript.put(nameKey, npcTranscripts);
        }
        npcTranscripts.put(textKey, text);
    }

    public String getTranslatedText(String name, String text, boolean keepColor) throws Exception
    {
        // ensures valid access to local npc transcript database
        if(transcript == null)
        {
            throw new Exception("NullTranscript");
        }

        // sanitizes arguments to access correct entry
        String textKey = sanitizeString(text, false);
        String nameKey = sanitizeString(name, false);
        
        if(transcript.containsKey(nameKey))
        {
            // retrieves all dialog lines from any given NPC
            Map<String, String> npcTranscripts = transcript.get(nameKey);
            if(npcTranscripts.containsKey(textKey))
            {
                if(keepColor)
                {
                    String prefix = getPrefix(text);
                    String suffix = getSuffix(text);
                    String output = "";
                
                    if(prefix != null)
                    {
                        output = prefix + npcTranscripts.get(textKey);
                    }
                    else
                    {
                        output = npcTranscripts.get(textKey);
                    }
                    
                    if(suffix != null)
                    {
                        output += suffix;
                    }
                    return output;
                }
                // retrieves a specific dialog line from the previously accessed NPC
                return npcTranscripts.get(textKey);
            }
            // if given text line was not found - raises an exception
            throw new Exception("LineNotFound");
        }
        // if given npc was not found on the database - adds it then returns the updated text
        throw new Exception("EntryNotFound");
    }
    
    public String getTranslatedName(String name, boolean keepColor) throws Exception
    {
        // ensures valid access to local npc transcript
        if(transcript == null)
        {
            throw new Exception("NullTranscript");
        }
        
        // polishes arguments to get transcript keys
        String nameKey = sanitizeString(name, false);
        if(transcript.containsKey(nameKey))
        {
            // retrieves all dialog lines from any given NPC
            Map<String, String> npcTranscripts = transcript.get(nameKey);
            
            if(keepColor)
            {
                try
                {
                    String prefix = getPrefix(name);
                    String suffix = getSuffix(name);
                    String output = "";
                    if(prefix != null)
                    {
                        output = prefix + npcTranscripts.get("name");
                    }
                    else
                    {
                        output = npcTranscripts.get("name");
                    }
                    
                    if(suffix != null)
                    {
                        output += suffix;
                    }
                    else
                    {
                        String suffixAlt = getSuffixAlt(name);
                        if(suffixAlt != null)
                        {
                            output += suffixAlt;
                            return output;
                        }
                    }
                    return output;
                    }
                catch(Exception f)
                {
                    return npcTranscripts.get("name");
                }
            }
            // retrieves translated npc name
            return npcTranscripts.get("name");
        }
        throw new Exception("NameNotFound");
    }
    
    private static String getPrefix(String input)
    {
        String regex = "^(<[^>]*>)(.*?)$";
        return getRegexGroup(input, regex, 1);
    }
    
    private static String getSuffix(String input)
    {
        String regex = "^(.*?)(<[^>]*>)$";
        return getRegexGroup(input, regex, 2);
    }
    
    private static String getSuffixAlt(String input)
    {
        Pattern pattern = Pattern.compile("<[^>]*>");
        Matcher matcher = pattern.matcher(input);
        
        int count = 0;
        while (matcher.find())
        {
            count++;
            if (count == 2)
            {
                return "<" + matcher.group().substring(1, matcher.group().length() - 1) + ">";
            }
        }
        
        return null;
    }
    
    private static String getRegexGroup(String input, String regex, int groupIndex)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        
        if (matcher.find())
        {
            return matcher.group(groupIndex);
        }
        
        return null;
    }
    
    private String sanitizeString(String input, boolean replaceAttributeNames) throws Exception
    {
        String regex = "<[^>]+>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        String sanitizedString = matcher.replaceAll("");
        sanitizedString = sanitizedString.toLowerCase();
        sanitizedString = Normalizer.normalize(sanitizedString, Normalizer.Form.NFD);
        sanitizedString = sanitizedString.replaceAll("[^\\p{ASCII}]", "");
        sanitizedString = sanitizedString.replaceAll("[^a-zA-Z0-9\\s]", "");
        sanitizedString = sanitizedString.replaceAll(" ", "");
        
        if(replaceAttributeNames)
        {
            /*%ITEM%
            %MONSTER%
            %REGION%
            %NAME%*/
        }
        
        return sanitizedString;
    }
    
    public void setLogger(LogHandler newLogger) {this.logger = newLogger;}
}