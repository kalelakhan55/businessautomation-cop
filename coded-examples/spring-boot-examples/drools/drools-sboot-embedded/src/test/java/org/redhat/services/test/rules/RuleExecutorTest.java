package org.redhat.services.test.rules;

import org.apache.tools.ant.taskdefs.condition.IsTrue;
import org.junit.Test;
import org.redhat.services.model.MortgageRequest;
import org.redhat.services.model.RuleResponse;
import org.redhat.services.rules.api.RuleExecutor;
import org.redhat.services.test.AppTestBase;
import org.springframework.beans.factory.annotation.Autowired;

import mortgages.mortgages.Applicant;
import mortgages.mortgages.Bankruptcy;
import mortgages.mortgages.IncomeSource;
import mortgages.mortgages.LoanApplication;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.Map;

public class RuleExecutorTest extends AppTestBase {

    @Autowired
    RuleExecutor ruleExecutor;

    @Test
    public void testHelloWorldRules(){
        RuleResponse ruleResponse = ruleExecutor.executeHelloWorldRules();
        assertThat(ruleResponse, notNullValue());
        assertThat(ruleResponse.getExecutionReference(), notNullValue());
        assertThat(ruleResponse.getPayload(), instanceOf(String.class));
        assertThat(ruleResponse.getPayload().toString(), equalTo("Hello World"));
        assertThat(ruleResponse.getRulesFired(), equalTo(1));
    }

    @Test
    public void testGoodByeRules(){
        String name = "Kris";

        RuleResponse ruleResponse = ruleExecutor.executeGoodbyeRules("Kris");
        assertThat(ruleResponse, notNullValue());
        assertThat(ruleResponse.getExecutionReference(), notNullValue());
        assertThat(ruleResponse.getPayload(), instanceOf(String.class));
        assertThat(ruleResponse.getPayload().toString(), equalTo("Goodbye " + name));
        assertThat(ruleResponse.getRulesFired(), equalTo(1));
    }

    @Test
    public void testMortgageRules(){
        Applicant applicant = new Applicant();
        applicant.setAge(17);
        applicant.setName("Test");
        applicant.setCreditRating("OK");
        IncomeSource incomeSource = new IncomeSource();
        LoanApplication loanApplication = new LoanApplication();
        Bankruptcy bankruptcy = new Bankruptcy();
        MortgageRequest request = new MortgageRequest(applicant, incomeSource, loanApplication, bankruptcy);

        Map<String, Object> ruleResponse = ruleExecutor.executeMortgageRules(request);
        assertThat(ruleResponse, notNullValue());
        assertThat(ruleResponse.get("Applicant"), notNullValue());
        assertThat(ruleResponse.get("Applicant"), instanceOf(Applicant.class));
        assertThat( ((Applicant)ruleResponse.get("Applicant")).getApproved(), is(true));
    }    
}
