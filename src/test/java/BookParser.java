
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.brsanthu.dataexporter.DataExporter;
import com.brsanthu.dataexporter.output.texttable.TextTableExporter;


/**
 * 
 * @author tim ng
 *
 */

public class BookParser {
	final static String BOOK_ONE_URL = "http://www.gutenberg.org/files/17192/17192-h/17192-h.htm";
	final static String BOOK_TWO_URL = "http://www.gutenberg.org/cache/epub/3748/pg3748.html";
    final static boolean DESC = false;
	

	
	public static void main(String[] args) {
		Map<String, Integer> book1Map = new HashMap<String,Integer>();
		Map<String, Integer> book2Map = new HashMap<String,Integer>();
		
		
		String book1 = getBookContent(BOOK_ONE_URL);	
		String book2 = getBookContent(BOOK_TWO_URL);
		countWordOccurrences(book1Map, book1);
		countWordOccurrences(book2Map,book2);
		
		List<Entry<String, Integer>> sortedBook1ListDesc = sortWordOccurrences(book1Map,DESC);
		List<Entry<String, Integer>> sortedBook2ListDesc = sortWordOccurrences(book2Map,DESC);
				
		//output results to log file
		outputToLog(sortedBook1ListDesc,sortedBook2ListDesc);
	}	

	
	/**
	 * Split book content into a string array, then put each word occurrences into a map
	 * @param bookMap
	 * @param s
	 */
	private static void countWordOccurrences(Map<String, Integer> bookMap,
			String s) {
		
		String regex = "^\\W*([^\\d]*)"; //matching any punctuation from beginning for each word
		Pattern pattern = Pattern.compile(regex);
	
		String[] words = s.split("[\\s+,;]+");  //split out words within comma, semicolon and space.
		
		for(String st:words){
	
		//ignore any word contains http or @
		if(st.contains(".") && (st.contains("http") || st.contains("www")) || st.contains("@")){
			continue;
		}
				
		//setup match pattern
		Matcher m = pattern.matcher(StringUtils.reverse(st)); //reverse string to strip out punctuation,space,symbols and tabs at the end of each word
		m.find();
		String word = m.group(1);
		m = pattern.matcher(StringUtils.reverse(word));  // reverse string back to normal then strip out punctuation,space,symbols and tabs at the beginning of each word
		m.find();
		word = m.group(1);

		//ignore any word that is Uppercase or blank space
		if( StringUtils.isAllUpperCase(word) || StringUtils.isBlank(word)){
			continue;
		}
		if(word.contains(".")){
			checkForSpecialCases(bookMap, word);  //last checking to cover input errors in the book
			continue;
		}
			word = word.toLowerCase();       //force all the word to lower case
			if (bookMap.containsKey(word)){								
				bookMap.put(word.toLowerCase(), bookMap.get(word) + 1);
			}
			else{
				bookMap.put(word, 1);
			}					
		 }

	}

	/**
	 * Output results to a log in a tabular format
	 * @param list1
	 * @param list2
	 */
	private static void outputToLog(List<Entry<String,Integer>>list1,
										List<Entry<String,Integer>>list2){
		
		StringWriter sw = new StringWriter();
		DataExporter exporter = new TextTableExporter(sw);
		
		String word1;
		Integer occurrence1;
		
		String word2;
		Integer occurrence2;
		
		
		exporter.addColumn("  Verne   ",
				"        Word        ",
				"  Occurrences  ", 
				"     ",  
				"  Poe  ",
				"        Word        ",
				"  Occurrences  ");
		
		//If list not empty, continue to loop through it
		while(!list1.isEmpty() || !list2.isEmpty()){
			
			//if first list is empty insert blank
			if(list1.isEmpty()){
				word1 = "";
				occurrence1 = null;
			}
			else{  // add row
				Entry<String, Integer> entry = list1.get(0);  //get first entry from list
				word1 = entry.getKey();
				occurrence1 = entry.getValue();		
				list1.remove(0);     //once entry has been processed, remove enty from list
			}
			//if list 2 is empty insert blank
			if(list2.isEmpty()){
				word2 = "";
				occurrence2 = null;
			}
			else{ // add row
				Entry<String, Integer> entry = list2.get(0);   //get first enty from list
				word2 = entry.getKey();
				occurrence2 = entry.getValue();
				list2.remove(0);                            // remove entry from list once entry has been processed
			}
			// add row to exporter
			exporter.addRow("    ", word2, occurrence2,"    ","      ", word1, occurrence1 );			
		}		
       
        exporter.finishExporting();
        
        
        //print results to log file
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("word_occurrence_count.txt", false)));
            out.println(sw.toString());
            out.close();
        } catch (IOException ioe) {
        	System.out.print(ioe.getMessage());
        }
	}

	
	/**
	 * Handle special punctuation in words.
	 * E.g., ignores a.m., p.m., u.s., etc...
	 * @param counts
	 * @param word
	 */
	private static void checkForSpecialCases(Map<String, Integer> counts,
			String word) {
		
		//split word that contains dot
		String[] otherWords = word.split("\\.");
		for(String text:otherWords){
			text = text.replaceAll("[^a-z\\sA-Z]", "");  //replace any non word charters with empty string
			if(StringUtils.isAllUpperCase(text)){
				continue;
			}
			
			if(text.length() > 1){
				if(counts.containsKey(text)){
					counts.put(text, counts.get(text) + 1);
				
				}
				else
				counts.put(text, 1);
			}							
		}
	}
	
	
	/**
	 * Download book in html format from url and extract book text.
	 * @param url
	 * @return
	 */
	private static String getBookContent(String url) {
	
		String rval = null;
		try {
			Document doc = Jsoup.connect(url).userAgent(
					"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0").get(); //http://www.gutenberg.org/files/17192/17192-h/17192-h.htm

			String bodyText = doc.body().text();	//get all the text within body tag					
			return bodyText;
			 
	  }
	 catch (IOException e) {
		e.printStackTrace();
	 }
		return rval;
	}
	
	
	/**
	 * sort results by descending order
	 * @param unsortMap
	 * @param order
	 * @return
	 */
	private static List<Entry<String, Integer>> sortWordOccurrences(Map<String, Integer> unsortMap, final boolean order)
    {
		

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        // Sorting words by the number of the times they occur 
        Collections.sort(list, new Comparator<Entry<String, Integer>>()
        {
            public int compare(Entry<String, Integer> o1,Entry<String, Integer> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        return list;
    }
}
