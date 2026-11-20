/**
 * One deliberate violation per rule of {@code com.tractorstore.ArchitectureRules}, used by {@code
 * ArchitectureRulesSelfTests}. Kept outside {@code com.tractorstore} so Spring never scans these
 * {@code @Entity}/{@code @Component} classes in integration tests.
 */
package archfixtures;
