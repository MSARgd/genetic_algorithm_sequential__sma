package ma.enset.ga.sma;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import ma.enset.ga.sequencial.GAUtils;
import ma.enset.ga.sequencial.Individual;

import java.lang.reflect.Array;
import java.util.*;

public class MainAgentGA extends Agent {
    List<AgentFitness> agentsFitness=new ArrayList<>();
    Random rnd=new Random();
    @Override
    protected void setup() {
        DFAgentDescription dfAgentDescription=new DFAgentDescription();
        ServiceDescription serviceDescription=new ServiceDescription();
        serviceDescription.setType("ga");
        dfAgentDescription.addServices(serviceDescription);
        try {
            DFAgentDescription[] agentsDescriptions = DFService.search(this, dfAgentDescription);
            System.out.println(agentsDescriptions.length);
            for (DFAgentDescription dfAD:agentsDescriptions) {
                agentsFitness.add(new AgentFitness(dfAD.getName(),0));
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        calculateFintness();
        SequentialBehaviour sequentialBehaviour=new SequentialBehaviour();
        sequentialBehaviour.addSubBehaviour(new Behaviour() {
            int cpt=0;
            @Override
            public void action() {
                ACLMessage receivedMSG = receive();
                if (receivedMSG!=null){
                    cpt++;
                    System.out.println(cpt);
                    int fintess=Integer.parseInt(receivedMSG.getContent());
                    AID sender=receivedMSG.getSender();
                    System.out.println(sender.getName()+" "+fintess);
                    setAgentFintess(sender,fintess);
                    if(cpt==GAUtils.POPULATION_SIZE){
                        Collections.sort(agentsFitness,Collections.reverseOrder());
                        showPopulation();
                    }
                }else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return cpt==GAUtils.CHROMOSOME_SIZE ;
            }

        });
        sequentialBehaviour.addSubBehaviour(new OneShotBehaviour() {
            int it=0;
            AgentFitness agent1;
            AgentFitness agent2;
            @Override
            public void action(){
                selection();
                crossover();
                Collections.sort(agentsFitness,Collections.reverseOrder());
                sendMessage(agentsFitness.get(0).getAid(),"chromosome",ACLMessage.REQUEST);
                ACLMessage aclMessage=blockingReceive();
                System.out.println(aclMessage.getContent().toCharArray());
                it++;
            }
                public void selection(){
                    System.out.println("Selection");
                    agent1=agentsFitness.get(0);
                    agent2=agentsFitness.get(1);
                    sendMessage(agent1.getAid(),"chromosome",ACLMessage.REQUEST);
                    sendMessage(agent2.getAid(),"chromosome",ACLMessage.REQUEST);
                }
                private  void crossover(){
                ACLMessage aclMessage1=blockingReceive();
                ACLMessage aclMessage2=blockingReceive();

                int pointCroisment=rnd.nextInt(GAUtils.CHROMOSOME_SIZE-2);
                pointCroisment++;
                char [] chromParent1=aclMessage1.getContent().toCharArray();
                char [] chromParent2=aclMessage2.getContent().toCharArray();
                char []chromosomeOffString1=new char[GAUtils.CHROMOSOME_SIZE];
                char []chromosomeOffString2=new char[GAUtils.CHROMOSOME_SIZE];
                for (int i=0;i<chromParent1.length;i++) {
                    chromosomeOffString1[i]=chromParent1[i];
                    chromosomeOffString2[i]=chromParent2[i];
                }
                for (int i=0;i<pointCroisment;i++) {
                    chromosomeOffString1[i]=chromParent2[i];
                    chromosomeOffString2[i]=chromParent1[i];
                }
                    int fitness=0;
                    for (int i=0;i<GAUtils.CHROMOSOME_SIZE;i++) {
                        if(chromosomeOffString1[i]==GAUtils.SOLUTION.charAt(i))
                            fitness+=1;
                    }
                    agentsFitness.get(GAUtils.CHROMOSOME_SIZE-2).setFitness(fitness);
                    fitness=0;
                    for (int i=0;i<GAUtils.CHROMOSOME_SIZE;i++) {
                        if(chromosomeOffString2[i]==GAUtils.SOLUTION.charAt(i))
                            fitness+=1;
                    }
                    agentsFitness.get(GAUtils.CHROMOSOME_SIZE-1).setFitness(fitness);
                sendMessage(agentsFitness.get(GAUtils.CHROMOSOME_SIZE-2).getAid(),new String(chromosomeOffString1),ACLMessage.REQUEST);
                sendMessage(agentsFitness.get(GAUtils.CHROMOSOME_SIZE-1).getAid(),new String(chromosomeOffString2),ACLMessage.REQUEST);
                ACLMessage recievedMSG1 =blockingReceive();
                ACLMessage recievedMSG2 =blockingReceive();
                setAgentFintess(recievedMSG1.getSender(),Integer.parseInt(recievedMSG1.getContent()));
                setAgentFintess(recievedMSG2.getSender(),Integer.parseInt(recievedMSG2.getContent()));



                }

            /*@Override
            public boolean done() {
                return true;//it==GAUtils.MAX_IT||agentsFitness.get(0).getFitness()==GAUtils.MAX_FITNESS;
            }*/
        });
        addBehaviour(sequentialBehaviour);
    }
private void calculateFintness(){
    ACLMessage message=new ACLMessage(ACLMessage.REQUEST);

    for (AgentFitness agf:agentsFitness) {
        message.addReceiver(agf.getAid());
    }
    message.setContent("fitness");
    send(message);

}
private void setAgentFintess(AID aid,int fitness){
        for (int i=0;i<GAUtils.POPULATION_SIZE;i++){
            if(agentsFitness.get(i).getAid().equals(aid)){
                agentsFitness.get(i).setFitness(fitness);
                System.out.println(fitness+"=:="+agentsFitness.get(i).getFitness());
                break;
            }
        }
}
private void sendMessage(AID aid,String content,int performative){
        ACLMessage aclMessage=new ACLMessage(performative);
        aclMessage.setContent(content);
        aclMessage.addReceiver(aid);
        send(aclMessage);
}
private void showPopulation(){
    for (AgentFitness agentFitness:agentsFitness) {
        System.out.println(agentFitness.getAid().getName()+" "+agentFitness.getFitness());
    }
}
}
