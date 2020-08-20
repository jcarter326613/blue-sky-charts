import { exit } from 'process'
import { SectionVersion } from '../models/section-version'
import { SectionVersionList } from '../models/section-version-list'

export class MetadataManager {
    public extractCurrentVersions(versionLists: Record<string, SectionVersionList>): Record<string, SectionVersion> {
        let returnMapping: Record<string, SectionVersion> = {}
        let now = new Date()

        for ( let mapName in versionLists ) {
            let versionList = versionLists[mapName]
            let latestCurrentVersion: SectionVersion | undefined
            let latestCurrentVersionDate: Date | undefined
            for ( let versionId in versionList.versions ) {
                let version = versionList.versions[versionId]
                let effectiveDate = this.extractDate(version.effectiveDate)
                let expirationDate = this.extractDate(version.expirationDate)
                if ( effectiveDate !== undefined && effectiveDate <= now &&
                    (expirationDate === undefined || expirationDate > now) ) {
                    if ( latestCurrentVersionDate === undefined || latestCurrentVersionDate < effectiveDate ) {
                        latestCurrentVersionDate = effectiveDate
                        latestCurrentVersion = version
                        latestCurrentVersion.version = versionId
                    }
                }
            }

            if ( latestCurrentVersion !== undefined ) {
                returnMapping[mapName] = latestCurrentVersion
            } else {
                console.error("Could not find active version of map")
                exit(1)
            }
        }

        return returnMapping
    }

    private extractDate(date: string | undefined): (Date | undefined) {
        if ( date === undefined || date.length < 10 ) {
            return undefined
        }

        return new Date(parseInt(date.substring(0, 4)), parseInt(date.substring(5, 7)) - 1, parseInt(date.substring(8, 10)))
    }
}