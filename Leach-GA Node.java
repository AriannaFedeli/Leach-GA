public class Node() extends Runnable {

	//progetto per il protocollo LEACH-GA
	boolean amIBS = false;
	boolean wasICHPrevCycle = false;
	int currentRound = 1;
	int timeSlot;
	double xCoord = 0;
	double yCoord = 0;
	ArrayList<Node> cluster = new ArrayList<Node>();
	ArrayList<int> clusterDTPL = new ArrayList<int>(); //For LEACH and LEACH-GA there are only 2 levels, low and high, 1 and 2
	Thread receiver;
	Thread aggregate;
	Thread feedback;
	Thread receiverCHJoinMePackets;
	
	public Node() {
		wasICHPrevCycle = false;
		//xCoord = getXCoordFromX();
		//YCoord = getYCoordFromY();
		cluster = new ArrayList<Node>();
		clusterDTPL = new ArrayList<int>();
	}
	public Node(boolean bs) {
		amIBS = bs;
		wasICHPrevCycle = false;
		//xCoord = getXCoordFromX();
		//yCoord = getYCoordFromY();
		cluster = new ArrayList<Node>();
		clusterDTPL = new ArrayList<int>();
	}

	public void Run() {

		//step one
			double p_set = getInputFromUserProbability();
			int n = getInputFromUserNumbNodes();
			
		//step two
			double e_init_s = this.getE_0(); //variabile che salva il valore iniziale della batteria 
			int s = this.getNodeID(); //variabile che prende il valore dell'id del nodo, nello pseudocodice: 1 <= id <= n

		//PREPARATION PHASE
		double r;
		double T_s;
		boolean cch_s; //candidate CH (si candida)
		boolean ch_s; // (diventa il capo, ehi boss)
			//step one
			if(e_init_s > 0  && rmod(1/p_set)!=0 ){
				//step two
				r = Math.Random(0,1);
				T_s = computeT_s(p_set);
				
				//step three
				if(r < T_s){
					//step four
					cch_s = true;
				}
				//step five
				else {
					//step six
					cch_s = false;
				}
				//step seven
				//nothing
			}
			//step eight
			//nothing

			//step nine
			sendToBS(this.getNodeID(), new Coords (getXCoord(), getYCoord()), cch_s); //tutti i nodi mandano messaggi al BS

			if (this.getAmIBS()) { //If I am the BS then I achieve these steps
				//step ten
				p_opt = GAinBS(); //GAinBS(p_opt); //Find p_opt by itself
			
				//step eleven
				this.radio.broadcast(p_opt); //ogni nodo ha un oggetto radio che interfaccia la radio //Spread out to all Nodes the value of p_opt
			}


			
		//SETUP PHASE

		setCurrentRound(1); //YOU CANNOT START FROM ROUND 0! LOOK AT THE GRAPHIC OF THE T(S) FUNCTION!
		do {
			
			receiverCHJoinMePackets = new Thread(ServiceCHReceiver, "ReceiverCH"); //It stores all the CH broadcast messages from CH, the ones that let it aggregate to a Cluster (if there is one)
			receiverCHJoinMePackets.start();
			
			//step one
			do{
				//step two
				r = Math.Random(0, 1);
				//step three
				if( e_init_s > 0 && rmod(1/p_opt) != 0) {
					//step four
					T_s = computeT_s(p_set);
					//step five
					if(r < T_s){
						//step six
						ch_s = true;
					}
					//step seven
					else{
						//step eight
						ch_s = false;
					}
					//step nine -> niente
				}
				//step ten -> niente
				//step eleven
				if(ch_s) {
					//receiverCHJoinMePackets custom code
					receiverCHJoinMePackets.terminate();
					receiverCHJoinMePackets.join();
					//end receiverCHJoinMePackets custom code
					//step twelve
					this.radio.broadcastIAmCH(); //broadcast max transmission power level
					//step 13
					this.radio.broadcastJoinMe(); //broadcast low transmission power level, low may be minimum
					//step 14
					cluster.clear(); //He reform the cluster, in the Algorithm there is any Cluster management section, then, if a node become CH who is within (low) range joins, then it's like reform the cluster
					setWasIChPrevCycle(true);
					break;
				}
				
				else {
					if (cluster.size() > 0) { //IF I am not a CH, then I join the closest CH that stands within my range
						Node closestCH = null; //Store the closest CH Node
						int DTPLclosestCH = Radio.GetMaxDTPL(); //Store the range, it is transmitted in Broadcast by the CH
						//the CH would Broadcast N messages for each DTPL that can be considered LOW RANGE, LEACH works with 2 DTPL (because it is a theoretical algorithm and should be implemented)
						for (int i = 0; i < cluster.size(); i++) { //cluster ArrayList is used by the receiverCHJoinMePackets Thread too, it just records all the Broadcast CH Nodes listened
							if (!this.isNodeACloserThanNodeB(DTPLclosest, i)) {
								closestCH = cluster.get(i);
								DTPLclosest = clusterDTPL.get(i);
							}
						}
						if (closestCH != null) {
							setWasIChPrevCycle(false); //Implicit in pseudocode
							receiverCHJoinMePackets.terminate(); //Terminate the Thread that were looking for Join Me Broadcast Packets sends from CHs
							receiverCHJoinMePackets.join(); //Join that sub-thread with the main thread, a way to DELETE it
							break; //Finded a CH
						}
					}
				}
				//step 15
			} while (true);
			 
			 

			 //STEADY-STATE PHASE
			 //step one
			if(ch_s) {
				//step two
				receiver = new Thread(ServiceReceiver, "Receiver");  //riceve dati dai membri
				receiver.start();
				//step three
				aggregate = new Thread(ServiceAggregator, "Aggregator");
				aggregate.start();
				//step four
				feedback = new Thread(ServiceFeedback, "Feedback"); //transmits received data
				feedback.start();
			}
			//step five
			else{
				setTimeSlot(findMyTimeSlot());
				//step six
				if(this.getTimeSlot() == this.radio.getTimeSlotNow()){
					//step seven
					this.radio.SleepMode(false);
					this.radio.sendData(); //computa dati di questo nodo RFD/FFD ed inviali, OUT OF PROTOCOL
				}
				//step eight
				else{
					//step nine
					try{
						this.radio.sleepMode(true);
					}
					catch(SleepModeAlreadyActiveException e ){
						//idgaf
						
					}
					
				}
				//step ten
				
			}
			//step eleven
			
			//LAST OPERATION BEFORE going back to SETUP Phase
			setCurrentRound(getCurrentRound + 1);
		//step twelve
		}
		//end
	
	}
		

		
		
		
		
		
		
		
	/**********************************************************************************
	 * Get and Set functions
	 **********************************************************************************/
	
	public boolean getAmIBS() {
		return amIBS;
	}
	
	private void setAmIBS(boolean input) {
		amIBS = input;
		return;
	}
	
	public boolean getWasIChPrevCycle() {
		return wasICHPrevCycle;
	}
	
	private void setWasIChPrevCycle(boolean input) {
		wasICHPrevCycle = input;
		return;
	}
	
	public int getCurrentRound() {
		return currentRound;
	}
	
	public void setCurrentRound(int input) {
		currentRound = input;
		return;
	}
	
	public int getTimeSlot() {
		return timeSlot;
	}
	
	private void setTimeSlot(int input) {
		timeSlot = input;
		return;
	}
	
	public double getXCoord() {
		return xCoord;
	}
	
	private void setXCoord(double input) {
		xCoord = input;
		return;
	}
	
	public double getYCoord() {
		return yCoord;
	}
	
	public void setYCoord(double input) {
		yCoord = input;
		return;
	}

	/**********************************************************************************
	 * Get and Set functions
	 **********************************************************************************/
	
	public double rmod(double divide){
		
		double r = this.getCurrentRound(); 
		return Math.Module(r,divide);
	}

	public double computeT_s(double p){
		
		if (!this.getWasIChPrevCycle()) {
			double output;
			double r = this.getCurrentRound();
			output = 1 - (p * Math.Module(r, (1 / p)));
			output = p / output;
			return output;
			//return  p/(1-p*(Math.Module(r,1/p)));
		}
		else{
			return 0;
		}
			
	}

	public void sendToBS(int id, Coords cordsXY, boolean cch){
		this.radio.radioSend(
				this.getBSNodeId(), //The ID of the receiver
				this.getBSNodeDiscreteTPL(), //The Discrete Transmission Power Level required to reach the BS
				id, //The ID of this Node
				cordsXY, //Coordinates only need to declare a theoretical position of the Node, not needed but it can be implemented
				cch); //The fact that this Node is trying to become a CH
		return;
	}

	public double GAinBS() {
		double p_opt = this.computeP_opt();
		return p_opt;
	}

	/*public double GAinBS(double p_opt) {
		//non so che cosa faccia
		//c'è una funzione molto brutta
		
		//PARAGRAPH 4
			//calcolo brutto scappo, ciao
			
	}*/

	public boolean isNodeACloserThanNodeB(
			int a, //DTPL of A
			int b) { //clusterDTPL ArrayList INDEX
		if (a == Radio.GetMaxDTPL()) {
			return false;
		}
		else if (a < clusterDTPL.get(b)) {
			return true;
		}
		else {
			return false;
		}
	}

	public int findMyTimeSlot() {
		while (true) for(i=0;i<feedbackList.size();i++){
			if (feedbackList.get(i).getNodeID() == this.getNodeID()){
				return feedbackList.get(i).getTimeSlot();
			}
		}
	}

	public double E_Pow_d_toCH_2() { //The shape of a cluster is a CIRCLE, instead of (6) we use (7)
		double output;
		output = Math.Pow(M, 2);
		output = output / Math.PI;
		output = output / k;
		return output;
	}

	public double computeP_opt() {
		return this.computeK_opt() / this.GetTotalNumberOfNodes();
	}

	public double computeK_opt() {
		double outputA;
		double outputB;
		double outputC;
		outputA = this.getTotalNumberOfNodes() / (2 * Math.PI);
		outputA = Math.Pow(outputA, 1/2);
		outputB = E_fs / E_mp;
		outputB = Math.Pow(outputB, 1/2);
		outputC = M / Math.Pow(d_toBS, 2);
		return outputA * outputB * outputC;
		
		//output = this.getTotalNumberOfNodes() / Math.PI;
		//output = Math.Pow(output, 0.5);
		//if (d_toBS < d_0) {
			
		//}
	}

	/**********************************************************************************
	 * NO ONE NEEDS THESE FUNCTIONS IN THE ALGORITHM
	 **********************************************************************************/

	public double E_Tx( //E_Tx(l,d) is the function that say how much energy requires the transmit amplifier to transmit l-bit over a distance d
			int l, //Length of the bit-string
			double d) { //distance in meters
		double output;
		
		double d_0; //The threshold distance
		d_0 = E_fs; //E_fs Amplifier Energy Consumption for d <= d_0
		d_0 = d_0 / E_mp; //E_mp Amplifier Energy Consumption for d >= d_0
		d_0 = Math.Pow(d_0, 0.5);
		
		output = l * E_elec; //E_elec rappresents the cost in energy to send or receive one bit
		
		if (d <= d_0) {
			output = output * E_fs * Math.Pow(d, 2);
		}
		else /*if (d >= d_0)*/{
			output = output * E_mp * Math.Pow(d, 4);
		}
		
		return output;
	}

	public double E_Rx( //E_Rx(l) is the function that say how much energy requires the radio to receive an l-bit message
			int l) { //Length of the bit-string
		return l * E_elec; //E_elec rappresents the cost in energy to send or receive one bit
	}

	public double E_CH( //E_CH(l,d) is the function that say how much energy requires, per round, the CH to receive data packets from member nodes, aggregate and forward to BS
			int l, //Length of the bit-string
			double d_toBS) { //distance in meters to BS
		double output;
		
		double d_0; //The threshold distance
		d_0 = E_fs; //E_fs Amplifer Energy Consumption for d_toBS <= d_0
		d_0 = d_0 / E_mp; //E_mp Amplifier Energy Consumption for d_toBS >= d_0
		d_0 = Math.Pow(d_0, 0.5);
		
		output = E_elec * ( //E_elec rappresents the cost in energy to send or receive one bit
				n / //n is the number of sensor nodes distributed uniformly in the MxM meter sensor field
				k - 1); //k is the number of clusters
		
		output = output + (E_DA * n / k); //E_DA rappresent the Energy dissipation for aggregating data
		output = output + E_elec;
		
		if (d_toBS <= d_0) {
			output = output + (E_fs * Math.Pow(d_toBS, 2));
			output = output * l;
		}
		else /*if (d_toBS >= d_0)*/ {
			output = output + (E_mp * Math.Pow(d_toBS, 4));
			output = output * l;
		}
		
		return output;
	}

	public double E_non-CH( //E_non-CH(l,d) is the function that say how much energy requires, per round, a non CH node to comunicate with its CH
			int l, //Length of the bit-string
			double d_toCH) { //distance in meters to CH
		double output;
		
		double d_0; //The threshold distance
		d_0 = E_fs; //E_fs Amplifier Energy Consumption for d_toCH <= d_0
		d_0 = d_0 / E_mp; //E_mp Amplifer Energy Consumption for d_toCH >= d_0
		d_0 = Math.Pow(d_0, 0.5);
		
		output = l * E_elec; //E_elec rappresents the cost in energy to send or receive one bit
		output = l * E_fs * Math.Pow(d_toCH, 2) + output; //No if condition because cluster nodes are within close range to the CH
		
		return output;
	}
}