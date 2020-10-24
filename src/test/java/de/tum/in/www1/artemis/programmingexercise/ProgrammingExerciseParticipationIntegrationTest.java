package de.tum.in.www1.artemis.programmingexercise;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.ProgrammingExercise;
import de.tum.in.www1.artemis.domain.ProgrammingSubmission;
import de.tum.in.www1.artemis.domain.Result;
import de.tum.in.www1.artemis.domain.Submission;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.participation.*;
import de.tum.in.www1.artemis.repository.*;

public class ProgrammingExerciseParticipationIntegrationTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private final String participationsBaseUrl = "/api/programming-exercise-participations/";

    private final String exercisesBaseUrl = "/api/programming-exercises/";

    @Autowired
    ProgrammingExerciseRepository programmingExerciseRepository;

    @Autowired
    ParticipationRepository participationRepository;

    @Autowired
    StudentParticipationRepository studentParticipationRepository;

    @Autowired
    TemplateProgrammingExerciseParticipationRepository templateProgrammingExerciseParticipationRepository;

    @Autowired
    SolutionProgrammingExerciseParticipationRepository solutionProgrammingExerciseParticipationRepository;

    @Autowired
    ResultRepository resultRepository;

    ProgrammingExercise programmingExercise;

    Participation programmingExerciseParticipation;

    @BeforeEach
    public void initTestCase() {
        database.addUsers(3, 2, 2);
        database.addCourseWithOneProgrammingExerciseAndTestCases();
        programmingExercise = programmingExerciseRepository.findAllWithEagerParticipations().get(0);
    }

    @AfterEach
    public void tearDown() {
        database.resetDatabase();
    }

    private static Stream<Arguments> argumentsForGetParticipationWithLatestResult() {
        ZonedDateTime someDate = ZonedDateTime.now();
        ZonedDateTime futureDate = ZonedDateTime.now().plusDays(3);
        ZonedDateTime pastDate = ZonedDateTime.now().minusDays(1);
        return Stream.of(
                // No assessmentType and no completionDate -> notFound
                Arguments.of(null, null, null, false),
                // Automatic result is always returned
                Arguments.of(AssessmentType.AUTOMATIC, null, null, true), Arguments.of(AssessmentType.AUTOMATIC, someDate, null, true),
                Arguments.of(AssessmentType.AUTOMATIC, someDate, futureDate, true), Arguments.of(AssessmentType.AUTOMATIC, someDate, pastDate, true),
                Arguments.of(AssessmentType.AUTOMATIC, null, futureDate, true), Arguments.of(AssessmentType.AUTOMATIC, null, pastDate, true),
                // Manual result without completion date (assessment was only saved but no submitted) is not returned
                Arguments.of(AssessmentType.MANUAL, null, null, false), Arguments.of(AssessmentType.MANUAL, null, futureDate, false),
                Arguments.of(AssessmentType.MANUAL, null, pastDate, false),
                // Manual result is not returned if completed and assessment due date has not passed
                Arguments.of(AssessmentType.MANUAL, someDate, futureDate, false),
                // Manual result is returned if completed and assessmentDue date has passed
                Arguments.of(AssessmentType.MANUAL, someDate, pastDate, true));
    }

    @ParameterizedTest
    @MethodSource("argumentsForGetParticipationWithLatestResult")
    @WithMockUser(username = "student1", roles = "USER")
    public void getParticipationWithLatestResultAsAStudent(AssessmentType assessmentType, ZonedDateTime completionDate, ZonedDateTime assessmentDueDate,
            boolean expectLastCreatedResult) throws Exception {
        programmingExercise.setAssessmentDueDate(assessmentDueDate);
        programmingExerciseRepository.save(programmingExercise);
        addStudentParticipationWithResult(assessmentType, completionDate);
        StudentParticipation participation = studentParticipationRepository.findAll().get(0);
        var expectedStatus = expectLastCreatedResult ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        request.get(participationsBaseUrl + participation.getId() + "/student-participation-with-latest-result-and-feedbacks", expectedStatus,
                ProgrammingExerciseStudentParticipation.class);
    }

    @ParameterizedTest
    @MethodSource("argumentsForGetParticipationWithLatestResult")
    @WithMockUser(username = "student1", roles = "USER")
    public void getParticipationWithLatestResult_multipleResultsAvailable(AssessmentType assessmentType, ZonedDateTime completionDate, ZonedDateTime assessmentDueDate,
            boolean expectLastCreatedResult) throws Exception {
        // Add an automatic result first
        var firstResult = addStudentParticipationWithResult(AssessmentType.AUTOMATIC, null);
        programmingExercise.setAssessmentDueDate(assessmentDueDate);
        programmingExerciseRepository.save(programmingExercise);
        // Add a parameterized second result
        var secondResult = database.addResultToParticipation(assessmentType, completionDate, programmingExerciseParticipation);
        StudentParticipation participation = studentParticipationRepository.findAll().get(0);

        // Expect the request to always be ok because it should at least return the first automatic result
        var requestedParticipation = request.get(participationsBaseUrl + participation.getId() + "/student-participation-with-latest-result-and-feedbacks", HttpStatus.OK,
                ProgrammingExerciseStudentParticipation.class);

        assertThat(requestedParticipation.getResults()).hasSize(1);
        var requestedResult = requestedParticipation.getResults().iterator().next();
        // Depending on the parameters we expect to get the first or the second created result from the server
        if (expectLastCreatedResult) {
            assertThat(requestedResult).isEqualTo(secondResult);
        }
        else {
            assertThat(requestedResult).isEqualTo(firstResult);
        }
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getParticipationWithLatestResultAsAnInstructor_noCompletionDate_notFound() throws Exception {
        addStudentParticipationWithResult(AssessmentType.MANUAL, null);
        StudentParticipation participation = studentParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/student-participation-with-latest-result-and-feedbacks", HttpStatus.NOT_FOUND,
                ProgrammingExerciseStudentParticipation.class);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void getLatestResultWithFeedbacksAsStudent() throws Exception {
        addStudentParticipationWithResult(null, null);
        StudentParticipation participation = studentParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/latest-result-with-feedbacks", HttpStatus.OK, Result.class);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void getLatestResultWithFeedbacksForTemplateParticipationAsTutorShouldReturnForbidden() throws Exception {
        addTemplateParticipationWithResult();
        TemplateProgrammingExerciseParticipation participation = templateProgrammingExerciseParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/latest-result-with-feedbacks", HttpStatus.FORBIDDEN, Result.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getLatestResultWithFeedbacksForTemplateParticipationAsTutor() throws Exception {
        addTemplateParticipationWithResult();
        TemplateProgrammingExerciseParticipation participation = templateProgrammingExerciseParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/latest-result-with-feedbacks", HttpStatus.OK, Result.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getLatestResultWithFeedbacksForTemplateParticipationAsInstructor() throws Exception {
        addTemplateParticipationWithResult();
        TemplateProgrammingExerciseParticipation participation = templateProgrammingExerciseParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/latest-result-with-feedbacks", HttpStatus.OK, Result.class);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void getLatestResultWithFeedbacksForSolutionParticipationAsTutorShouldReturnForbidden() throws Exception {
        addSolutionParticipationWithResult();
        SolutionProgrammingExerciseParticipation participation = solutionProgrammingExerciseParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/latest-result-with-feedbacks", HttpStatus.FORBIDDEN, Result.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getLatestResultWithFeedbacksForSolutionParticipationAsTutor() throws Exception {
        addSolutionParticipationWithResult();
        SolutionProgrammingExerciseParticipation participation = solutionProgrammingExerciseParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/latest-result-with-feedbacks", HttpStatus.OK, Result.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getLatestResultWithFeedbacksForSolutionParticipationAsInstructor() throws Exception {
        addSolutionParticipationWithResult();
        SolutionProgrammingExerciseParticipation participation = solutionProgrammingExerciseParticipationRepository.findAll().get(0);
        request.get(participationsBaseUrl + participation.getId() + "/latest-result-with-feedbacks", HttpStatus.OK, Result.class);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void getLatestPendingSubmissionIfExists_student() throws Exception {
        ProgrammingSubmission submission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission = database.addProgrammingSubmission(programmingExercise, submission, "student1");
        request.get(participationsBaseUrl + submission.getParticipation().getId() + "/latest-pending-submission", HttpStatus.OK, ProgrammingSubmission.class);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getLatestPendingSubmissionIfExists_ta() throws Exception {
        ProgrammingSubmission submission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission = database.addProgrammingSubmission(programmingExercise, submission, "student1");
        request.get(participationsBaseUrl + submission.getParticipation().getId() + "/latest-pending-submission", HttpStatus.OK, ProgrammingSubmission.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getLatestPendingSubmissionIfExists_instructor() throws Exception {
        ProgrammingSubmission submission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission = database.addProgrammingSubmission(programmingExercise, submission, "student1");
        request.get(participationsBaseUrl + submission.getParticipation().getId() + "/latest-pending-submission", HttpStatus.OK, ProgrammingSubmission.class);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void getLatestPendingSubmissionIfNotExists_student() throws Exception {
        // Submission has a result, therefore not considered pending.
        Result result = resultRepository.save(new Result());
        ProgrammingSubmission submission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission.setResult(result);
        submission = database.addProgrammingSubmission(programmingExercise, submission, "student1");
        Submission returnedSubmission = request.getNullable(participationsBaseUrl + submission.getParticipation().getId() + "/latest-pending-submission", HttpStatus.OK,
                ProgrammingSubmission.class);
        assertThat(returnedSubmission).isEqualTo(submission);
    }

    @Test
    @WithMockUser(username = "tutor1", roles = "TA")
    public void getLatestPendingSubmissionIfNotExists_ta() throws Exception {
        // Submission has a result, therefore not considered pending.
        Result result = resultRepository.save(new Result());
        ProgrammingSubmission submission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission.setResult(result);
        submission = database.addProgrammingSubmission(programmingExercise, submission, "student1");
        Submission returnedSubmission = request.getNullable(participationsBaseUrl + submission.getParticipation().getId() + "/latest-pending-submission", HttpStatus.OK,
                ProgrammingSubmission.class);
        assertThat(returnedSubmission).isEqualTo(submission);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getLatestPendingSubmissionIfNotExists_instructor() throws Exception {
        // Submission has a result, therefore not considered pending.
        Result result = resultRepository.save(new Result());
        ProgrammingSubmission submission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission.setResult(result);
        submission = database.addProgrammingSubmission(programmingExercise, submission, "student1");
        Submission returnedSubmission = request.getNullable(participationsBaseUrl + submission.getParticipation().getId() + "/latest-pending-submission", HttpStatus.OK,
                ProgrammingSubmission.class);
        assertThat(returnedSubmission).isEqualTo(submission);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void getLatestSubmissionsForExercise_instructor() throws Exception {
        ProgrammingSubmission submission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission = database.addProgrammingSubmission(programmingExercise, submission, "student1");
        ProgrammingSubmission submission2 = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(61L));
        submission2 = database.addProgrammingSubmission(programmingExercise, submission2, "student2");
        ProgrammingSubmission notPendingSubmission = (ProgrammingSubmission) new ProgrammingSubmission().submissionDate(ZonedDateTime.now().minusSeconds(55L));
        database.addProgrammingSubmissionWithResult(programmingExercise, notPendingSubmission, "student3");
        Map<Long, ProgrammingSubmission> submissions = new HashMap<>();
        submissions.put(submission.getParticipation().getId(), submission);
        submissions.put(submission2.getParticipation().getId(), submission2);
        submissions.put(notPendingSubmission.getParticipation().getId(), null);
        Map<Long, ProgrammingSubmission> returnedSubmissions = request.getMap(exercisesBaseUrl + programmingExercise.getId() + "/latest-pending-submissions", HttpStatus.OK,
                Long.class, ProgrammingSubmission.class);
        assertThat(returnedSubmissions).isEqualTo(submissions);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void testGetLatestSubmissionsForExercise_studentForbidden() throws Exception {
        request.getMap(exercisesBaseUrl + programmingExercise.getId() + "/latest-pending-submissions", HttpStatus.FORBIDDEN, Long.class, ProgrammingSubmission.class);
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetParticipationWithResultsForStudentParticipation_success() throws Exception {
        database.addGradingInstructionsToExercise(programmingExercise);
        programmingExerciseRepository.save(programmingExercise);
        addStudentParticipationWithResult(AssessmentType.AUTOMATIC, ZonedDateTime.now());
        StudentParticipation participation = studentParticipationRepository.findAll().get(0);

        ProgrammingExerciseStudentParticipation response = request.get(participationsBaseUrl + participation.getId() + "/student-participation-with-results-and-feedbacks",
                HttpStatus.OK, ProgrammingExerciseStudentParticipation.class);
        ProgrammingExercise exercise = (ProgrammingExercise) response.getExercise();

        assertThat(exercise.getGradingCriteria().get(0).getStructuredGradingInstructions().size()).isEqualTo(1);
        assertThat(exercise.getGradingCriteria().get(1).getStructuredGradingInstructions().size()).isEqualTo(1);
        assertThat(response.getResults().iterator().next().getAssessmentType()).isEqualTo(AssessmentType.AUTOMATIC);
        assertThat(response.getResults().iterator().next().getResultString()).isEqualTo("x of y passed");
    }

    @Test
    @WithMockUser(username = "instructor1", roles = "INSTRUCTOR")
    public void testGetParticipationWithResultsForStudentParticipation_notFound() throws Exception {
        StudentParticipation participation = database.createAndSaveParticipationForExercise(programmingExercise, "student1");
        request.get(participationsBaseUrl + participation.getId() + "/student-participation-with-results-and-feedbacks", HttpStatus.NOT_FOUND,
                ProgrammingExerciseStudentParticipation.class);
    }

    @Test
    @WithMockUser(username = "stidemt1", roles = "USER")
    public void testGetParticipationWithResultsForStudentParticipation_forbidden() throws Exception {
        request.getMap(exercisesBaseUrl + programmingExercise.getId() + "/latest-pending-submissions", HttpStatus.FORBIDDEN, Long.class, ProgrammingSubmission.class);
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void checkIfParticipationHasResult_withResult_returnsTrue() throws Exception {
        addStudentParticipationWithResult(null, null);
        database.addResultToParticipation(null, null, programmingExerciseParticipation);

        final var response = request.get("/api/programming-exercise-participations/" + programmingExerciseParticipation.getId() + "/has-result", HttpStatus.OK, Boolean.class);

        assertThat(response).isTrue();
    }

    @Test
    @WithMockUser(username = "student1", roles = "USER")
    public void checkIfParticipationHasResult_withoutResult_returnsFalse() throws Exception {
        programmingExerciseParticipation = database.addStudentParticipationForProgrammingExercise(programmingExercise, "student1");

        final var response = request.get("/api/programming-exercise-participations/" + programmingExerciseParticipation.getId() + "/has-result", HttpStatus.OK, Boolean.class);

        assertThat(response).isFalse();
    }

    private Result addStudentParticipationWithResult(AssessmentType assessmentType, ZonedDateTime completionDate) {
        programmingExerciseParticipation = database.addStudentParticipationForProgrammingExercise(programmingExercise, "student1");
        return database.addResultToParticipation(assessmentType, completionDate, programmingExerciseParticipation);
    }

    private void addTemplateParticipationWithResult() {
        programmingExerciseParticipation = database.addTemplateParticipationForProgrammingExercise(programmingExercise).getTemplateParticipation();
        database.addResultToParticipation(AssessmentType.AUTOMATIC, null, programmingExerciseParticipation);
    }

    private void addSolutionParticipationWithResult() {
        programmingExerciseParticipation = database.addSolutionParticipationForProgrammingExercise(programmingExercise).getSolutionParticipation();
        database.addResultToParticipation(AssessmentType.AUTOMATIC, null, programmingExerciseParticipation);
    }

}
