package com.amazonaws.samples.ujr.aws.swf.hello;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClientBuilder;
import com.amazonaws.services.simpleworkflow.model.ActivityType;
import com.amazonaws.services.simpleworkflow.model.CompleteWorkflowExecutionDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.PollForDecisionTaskRequest;
import com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.TaskList;

public class WorkflowWorker {
	private static final AmazonSimpleWorkflow swf =
	        AmazonSimpleWorkflowClientBuilder.defaultClient();

	public static void main(String[] args) {
        PollForDecisionTaskRequest task_request =
            new PollForDecisionTaskRequest()
                .withDomain(HelloTypes.DOMAIN)
                .withTaskList(new TaskList().withName(HelloTypes.TASKLIST));

        while (true) {
            System.out.println(
                    "Polling for a decision task from the tasklist '" +
                    HelloTypes.TASKLIST + "' in the domain '" +
                    HelloTypes.DOMAIN + "'.");

            DecisionTask task = swf.pollForDecisionTask(task_request);

            String taskToken = task.getTaskToken();
            if (taskToken != null) {
                try {
                    executeDecisionTask(taskToken, task.getEvents());
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        }
    }

    /**
     * The goal of this workflow is to execute at least one HelloActivity successfully.
     *
     * We pass the workflow execution's input to the activity, and we use the activity's result
     * as the output of the workflow.
     */
    private static void executeDecisionTask(String taskToken, List<HistoryEvent> events)
            throws Throwable {
        List<Decision> decisions = new ArrayList<Decision>();
        String workflow_input = null;
        int scheduled_activities = 0;
        int open_activities = 0;
        boolean activity_completed = false;
        String result = null;

        System.out.println("Executing the decision task for the history events: [");
        for (HistoryEvent event : events) {
            System.out.println("  " + event);
            switch(event.getEventType()) {
                case "WorkflowExecutionStarted":
                    workflow_input =
                        event.getWorkflowExecutionStartedEventAttributes()
                             .getInput();
                    break;
                case "ActivityTaskScheduled":
                    scheduled_activities++;
                    break;
                case "ScheduleActivityTaskFailed":
                    scheduled_activities--;
                    break;
                case "ActivityTaskStarted":
                    scheduled_activities--;
                    open_activities++;
                    break;
                case "ActivityTaskCompleted":
                    open_activities--;
                    activity_completed = true;
                    result = event.getActivityTaskCompletedEventAttributes()
                                  .getResult();
                    break;
                case "ActivityTaskFailed":
                    open_activities--;
                    break;
                case "ActivityTaskTimedOut":
                    open_activities--;
                    break;
            }
        }
        System.out.println("]");

        if (activity_completed) {
            decisions.add(
                new Decision()
                    .withDecisionType(DecisionType.CompleteWorkflowExecution)
                    .withCompleteWorkflowExecutionDecisionAttributes(
                        new CompleteWorkflowExecutionDecisionAttributes()
                            .withResult(result)));
        } else {
            if (open_activities == 0 && scheduled_activities == 0) {

                ScheduleActivityTaskDecisionAttributes attrs =
                    new ScheduleActivityTaskDecisionAttributes()
                        .withActivityType(new ActivityType()
                            .withName(HelloTypes.ACTIVITY)
                            .withVersion(HelloTypes.ACTIVITY_VERSION))
                        .withActivityId(UUID.randomUUID().toString())
                        .withInput(workflow_input);

                decisions.add(
                        new Decision()
                            .withDecisionType(DecisionType.ScheduleActivityTask)
                            .withScheduleActivityTaskDecisionAttributes(attrs));
            } else {
                // an instance of HelloActivity is already scheduled or running. Do nothing, another
                // task will be scheduled once the activity completes, fails or times out
            }
        }

        System.out.println("Exiting the decision task with the decisions " + decisions);

        swf.respondDecisionTaskCompleted(
            new RespondDecisionTaskCompletedRequest()
                .withTaskToken(taskToken)
                .withDecisions(decisions));
    }

}
