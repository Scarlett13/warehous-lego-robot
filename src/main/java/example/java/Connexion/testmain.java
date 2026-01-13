package example.java.Connexion;

public class testmain {
    public static void main(String[] args){
        Device2.init();

        while(true){
           try{
               System.out.println("Moving forward...");
               Device2.forward();
               Thread.sleep(2000);  // move 2 seconds

               System.out.println("Stopping...");
               Device2.stop();
               Thread.sleep(1000);  // pause

               System.out.println("Moving backward...");
               Device2.backward();
               Thread.sleep(2000);

               System.out.println("Stopping...");
               Device2.stop();
               Thread.sleep(1000);
           }catch(Exception e){

           }
        }
    }

    void test(){

    }
}
