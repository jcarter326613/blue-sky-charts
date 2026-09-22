
import { Condition } from './condition';

export class ConditionResponse {
    public oldestDataAgeSeconds: number | undefined;
    public conditions: Array<Condition> | undefined;
}