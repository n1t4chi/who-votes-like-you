package vote.fetcher.message

import model.Cadence

sealed class ProducedCadence(val cadence: Cadence) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ProducedCadence
        
        if (cadence != other.cadence) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        return cadence.hashCode()
    }
    
    override fun toString(): String {
        return this.javaClass.simpleName+"($cadence)"
    }
}