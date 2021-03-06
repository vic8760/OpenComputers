package li.cil.oc.integration.forestry;

import com.google.common.collect.Maps;
import forestry.api.genetics.IAllele;
import forestry.api.genetics.IAlleleSpecies;
import forestry.api.genetics.IMutation;
import li.cil.oc.api.driver.Converter;

import java.util.Map;

public class ConverterIAlleles implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof IMutation) {
            IMutation mutation = (IMutation) value;

            IAllele allele1 = mutation.getAllele0();
            if (allele1 instanceof IAlleleSpecies) {
                Map<Object, Object> allelMap1 = Maps.newHashMap();
                convert(allele1, allelMap1);
                output.put("allele1", allelMap1);
            }
            IAllele allele2 = mutation.getAllele1();
            if (allele2 instanceof IAlleleSpecies) {
                Map<Object, Object> allelMap2 = Maps.newHashMap();
                convert(allele2, allelMap2);
                output.put("allele2", allelMap2);
            }
            output.put("chance", mutation.getBaseChance());
            output.put("specialConditions", mutation.getSpecialConditions().toArray());
        }

        if (value instanceof IAlleleSpecies) {
            convertAlleleSpecies((IAlleleSpecies) value, output);
        }
    }

    private void convertAlleleSpecies(final IAlleleSpecies value, final Map<Object, Object> output) {
        output.put("name", value.getName());
        output.put("uid", value.getUID());
    }
}
