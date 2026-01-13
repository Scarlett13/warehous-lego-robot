//----------------------------------------------------------------------------
// Copyright (C) 2013 Ingrid Nunes
// 
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
// 
// To contact the authors:
// http://inf.ufrgs.br/prosoft/bdi4jade/
//
//----------------------------------------------------------------------------

package example.examples.planselection;

import bdi4jade.annotation.Belief;
import bdi4jade.examples.planselection.GenericValueFunction;
import bdi4jade.examples.planselection.Softgoals;
import bdi4jade.examples.planselection.TransportationAgent;
import bdi4jade.examples.planselection.TransportationPlan;
import bdi4jade.extension.planselection.utilitybased.SoftgoalPreferences;
import bdi4jade.plan.Plan.EndState;
import bdi4jade.plan.planbody.AbstractPlanBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Random;

/**
 * @author Ingrid Nunes
 */
public class TransportationPlanBody extends AbstractPlanBody {

	class Scenario {

		Double comfort;
		Double cost;
		Boolean crashed;
		Random rand;
		Boolean robbed;
		Double timeTaken;

		public Scenario() {
			this.rand = new Random(System.currentTimeMillis());
			this.crashed = occurred(rand, plan.getCrashProbability());
			this.comfort = plan.getComfort();
			this.timeTaken = bdi4jade.examples.planselection.TransportationPlan.MAX_TIME_TAKEN;
			this.cost = plan.getCost();
			if (!crashed) {
				this.timeTaken = new Double(rand.nextInt(plan.getMaxTime()
						- plan.getMinTime())
						+ plan.getMinTime());
			} else {
				if (!plan.isCostConstant())
					this.cost = 1.0;
			}
			this.robbed = occurred(rand, plan.getBeingRobbedProbability());
		}

		public Double getSatisfaction() {
			double safetySatisfaction = crashed ? 0.0 : 1.0;
			log.debug("safetySatisfaction = " + safetySatisfaction);

			double costSatisfaction = 1 - cost;
			log.debug("costSatisfaction = " + costSatisfaction);

			double comfortSatisfaction = comfort;
			log.debug("comfortSatisfaction = " + comfortSatisfaction);

			double performanceSatisfaction = crashed ? 0.0
					: 1 - (timeTaken / bdi4jade.examples.planselection.TransportationPlan.MAX_TIME_TAKEN);
			log.debug("performanceSatisfaction = " + performanceSatisfaction);

			double securitySatisfaction = robbed ? 0.0 : 1.0;
			log.debug("securitySatisfaction = " + securitySatisfaction);

			double satisfaction = preferences
					.getPreferenceForSoftgoal(bdi4jade.examples.planselection.Softgoals.SAFETY)
					* safetySatisfaction
					+ preferences.getPreferenceForSoftgoal(bdi4jade.examples.planselection.Softgoals.COST)
					* costSatisfaction
					+ preferences.getPreferenceForSoftgoal(bdi4jade.examples.planselection.Softgoals.COMFORT)
					* comfortSatisfaction
					+ preferences
							.getPreferenceForSoftgoal(bdi4jade.examples.planselection.Softgoals.PERFORMANCE)
					* performanceSatisfaction
					+ preferences.getPreferenceForSoftgoal(Softgoals.SECURITY)
					* securitySatisfaction;
			log.debug("Total Satisfaction = " + satisfaction);
			return satisfaction;
		}

		private boolean occurred(Random rand, double probability) {
			double number = rand.nextDouble();
			return (number < probability);
		}

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("[ crashed = ").append(crashed).append(", cost = ")
					.append(cost).append(", time taken = ").append(timeTaken)
					.append(", robbed = ").append(robbed).append(" ]");
			return sb.toString();
		}

	}

	private static final long serialVersionUID = -9039447524062487795L;

	private Log log;
	private bdi4jade.examples.planselection.TransportationPlan plan;
	@Belief(name = SoftgoalPreferences.NAME)
	private SoftgoalPreferences preferences;
	@Belief(name = TransportationAgent.SATISFACTION)
	private bdi4jade.belief.Belief<String, GenericValueFunction<Integer>> satisfaction;

	public void action() {
		log.debug("Plan executed: " + this.plan.getId());
		Scenario scenario = new Scenario();
		double satisfaction = scenario.getSatisfaction();
		this.satisfaction.getValue().addValue(
				this.satisfaction.getValue().getCount() + 1, satisfaction);
		log.debug("Plan finished!");
		setEndState(EndState.SUCCESSFUL);
	}

	public void onStart() {
		this.log = LogFactory.getLog(this.getClass());
		this.plan = (TransportationPlan) getPlan();
	}

}
