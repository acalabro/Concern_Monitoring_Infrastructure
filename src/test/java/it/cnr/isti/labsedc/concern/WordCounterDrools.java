package it.cnr.isti.labsedc.concern;

import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.EntryPoint;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.drools.kiesession.rulebase.InternalKnowledgeBase;
import org.drools.kiesession.rulebase.KnowledgeBaseFactory;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import java.util.*;

public class WordCounterDrools {
	private static KnowledgeBuilder kbuilder;
	private String instanceName;
	private String staticRuleToLoadAtStartup;
	private boolean started = false;
	private String username;
	private String password;

    private static Collection<KiePackage> pkgs;
    private static InternalKnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
    private static KieSession ksession;
	private EntryPoint eventStream;
	private boolean isUsingJMS = true;
	public static ArrayList<String> rulesNames;
	public static int totalRulesLoaded = 0;
	public static String lastRuleLoadedName;
	
    public static void main(String[] args) {
    	try{
			kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		}catch(Exception e) {
			System.out.println(e.getCause() + "\n"+
			e.getMessage());
		}
    	Resource drlToLoad = ResourceFactory.newFileResource(System.getProperty("user.dir")+ "/src/main/resources/testRule.drl");
        kbuilder.add(drlToLoad, ResourceType.DRL);
        pkgs = kbuilder.getKnowledgePackages();
        
        long a = System.currentTimeMillis();
        kbase.addPackages(pkgs);
        ksession = kbase.newKieSession();
        
        String[] sentences = {"To be, or not to be,--that is the question:--", "Whether 'tis nobler in the mind to suffer", "The slings and arrows of outrageous fortune", "Or to take arms against a sea of troubles,", "And by opposing end them?--To die,--to sleep,--", "No more; and by a sleep to say we end", "The heartache, and the thousand natural shocks", "That flesh is heir to,--'tis a consummation", "Devoutly to be wish'd. To die,--to sleep;--", "To sleep! perchance to dream:--ay, there's the rub;", "For in that sleep of death what dreams may come,", "When we have shuffled off this mortal coil,", "Must give us pause: there's the respect", "That makes calamity of so long life;", "For who would bear the whips and scorns of time,", "The oppressor's wrong, the proud man's contumely,", "The pangs of despis'd love, the law's delay,", "The insolence of office, and the spurns", "That patient merit of the unworthy takes,", "When he himself might his quietus make", "With a bare bodkin? who would these fardels bear,", "To grunt and sweat under a weary life,", "But that the dread of something after death,--", "The undiscover'd country, from whose bourn", "No traveller returns,--puzzles the will,", "And makes us rather bear those ills we have", "Than fly to others that we know not of?", "Thus conscience does make cowards of us all;", "And thus the native hue of resolution", "Is sicklied o'er with the pale cast of thought;", "And enterprises of great pith and moment,", "With this regard, their currents turn awry,", "And lose the name of action.--Soft you now!", "The fair Ophelia!--Nymph, in thy orisons", "Be all my sins remember'd."};
        List<String> wordsList = new ArrayList<>();
        Map<String, Integer> wordCount = new LinkedHashMap<>();

        for (String sentence : sentences) {
            for (String word : sentence.split(" ")) {
                wordsList.add(word.toLowerCase());
            }
        }

        for (String word : wordsList) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            ksession.insert(new WordOccurrence(word, wordCount.get(word)));
        }
        ksession.fireAllRules();
        ksession.dispose();
        
        System.out.println("ExecutionTime: " + (System.currentTimeMillis()-a));
    }
}

