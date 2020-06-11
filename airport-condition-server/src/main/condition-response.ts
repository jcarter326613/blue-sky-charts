
import { Condition } from './condition';

export class ConditionResponse {
    public oldestIssueDate: number | undefined;
    public conditions: Array<Condition> | undefined;
}