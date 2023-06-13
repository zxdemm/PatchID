package hk.polyu.comp.jaid.fixer;

import hk.polyu.comp.jaid.fixer.config.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Max PEI.
 */
public class Session {

    private static Session session;
    public static List<Session> sessions = new ArrayList<>();
    public static void initSession(Config config){
//        if(session != null)
//            //改成return
////            throw new IllegalStateException();
//            return;
        try {
            session = new Session(config);
            sessions.add(session);
        }
        catch(Exception e){
            // Exit.
            throw new IllegalStateException("Failed to initialize the JAID session.");
        }
    }

    public static Session getSession(){
        if(session == null)
            throw new IllegalStateException();

        return sessions.get(sessions.size() - 1) ;
    }

    private Session(Config config) throws Exception{
        this.config = config;
    }

    private Config config;

    public Config getConfig() {
        return config;
    }

}
