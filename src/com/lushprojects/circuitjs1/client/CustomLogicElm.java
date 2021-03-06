package com.lushprojects.circuitjs1.client;

import com.google.gwt.user.client.ui.Button;

public class CustomLogicElm extends ChipElm {
    String modelName;
    int postCount;
    int inputCount, outputCount;
    CustomLogicModel model;
    boolean lastValues[];
    boolean patternValues[];
    static String lastModelName = "default";
    
    public CustomLogicElm(int xx, int yy) {
	super(xx, yy);
	modelName = lastModelName;
	setupPins();
    }

    public CustomLogicElm(int xa, int ya, int xb, int yb, int f,
            StringTokenizer st) {
	super(xa, ya, xb, yb, f, st);
	modelName = st.nextToken();
	updateModels();
	int i;
	for (i = 0; i != getPostCount(); i++) {
	    if (pins[i].output) {
		volts[i] = new Double(st.nextToken()).doubleValue();
		pins[i].value = volts[i] > 2.5;
	    }
	}
    }
    
    String dump() {
	String s = super.dump();
	s += " " + modelName;

	// the code to do this in ChipElm doesn't work here because we don't know
	// how many pins to read until we read the model name!  So we have to
	// duplicate it here.
        int i;
        for (i = 0; i != getPostCount(); i++) {
            if (pins[i].output)
                s += " " + volts[i];
        }
	return s;
    }
    
    String dumpModel() {
	if (model.dumped)
	    return "";
	return model.dump();
    }
    
    public void updateModels() {
	model = CustomLogicModel.getModelWithNameOrCopy(modelName, model);
	setupPins();
	allocNodes();
	setPoints();
    }
    
    @Override
    void setupPins() {
	if (modelName == null) {
	    postCount = bits;
	    allocNodes();
	    return;
	}
	
	model = CustomLogicModel.getModelWithName(modelName);
	inputCount = model.inputs.length;
	outputCount = model.outputs.length;
	sizeY = inputCount > outputCount ? inputCount : outputCount;
	if (sizeY == 0)
	    sizeY = 1;
	sizeX = 2;
	postCount = inputCount+outputCount;
	pins = new Pin[postCount];
	int i;
	for (i = 0; i != inputCount; i++) {
	    pins[i] = new Pin(i, SIDE_W, model.inputs[i]);
	    fixPinName(pins[i]);
	}
	for (i = 0; i != outputCount; i++) {
	    pins[i+inputCount] = new Pin(i, SIDE_E, model.outputs[i]);
	    pins[i+inputCount].output = true;
	    fixPinName(pins[i+inputCount]);
	}
	lastValues = new boolean[postCount];
	patternValues = new boolean[26];
    }

    void fixPinName(Pin p) {
	if (p.text.startsWith("/")) {
	    p.text = p.text.substring(1);
	    p.lineOver = true;
	}
	if (p.text.compareToIgnoreCase("clk") == 0) {
	    p.text = "";
	    p.clock = true;
	}
    }
    
    int getPostCount() { return postCount; }
    
    @Override
    int getVoltageSourceCount() {
	return outputCount;
    }

    void execute() {
	int i;
	for (i = 0; i != model.rulesLeft.size(); i++) {
	    // check for a match
	    String rl = model.rulesLeft.get(i);
	    int j;
	    for (j = 0; j != rl.length(); j++) {
		char x = rl.charAt(j);
		if (x == '0' || x == '1') {
		    if (pins[j].value == (x == '1'))
			continue;
		    break;
		}
		
		// don't care
		if (x == '?')
		    continue;
		
		// up transition
		if (x == '+') {
		    if (pins[j].value && !lastValues[j])
			continue;
		    break;
		}
		
		// down transition
		if (x == '-') {
		    if (!pins[j].value && lastValues[j])
			continue;
		    break;
		}
		
		// save pattern values
		if (x >= 'a' && x <= 'z') {
		    patternValues[x-'a'] = pins[j].value;
		    continue;
		}
		
		// compare pattern values
		if (x >= 'A' && x <= 'z') {
		    if (patternValues[x-'A'] != pins[j].value)
			break;
		    continue;
		}
	    }
	    if (j != rl.length())
		continue;
	    
	    // success
	    String rr = model.rulesRight.get(i);
	    for (j = 0; j != rr.length(); j++) {
		char x = rr.charAt(j);
		if (x >= 'a' && x <= 'z')
		    pins[j+inputCount].value = patternValues[x-'a'];
		else
		    pins[j+inputCount].value = (x == '1');
	    }
	    
	    // save values for transition checking
	    for (j = 0; j != postCount; j++)
		lastValues[j] = pins[j].value;
	    break;
	}
    }
    
    public EditInfo getEditInfo(int n) {
	if (n == 2) {
	    EditInfo ei = new EditInfo("Model Name", 0, -1, -1);
	    ei.text = modelName;
	    return ei;
	}
	if (n == 3) {
            EditInfo ei = new EditInfo("", 0, -1, -1);
            ei.button = new Button(sim.LS("Edit Model"));
            return ei;
	}
	return super.getEditInfo(n);
    }
    
    public void setEditValue(int n, EditInfo ei) {
	if (n == 2) {
	    modelName = lastModelName = ei.textf.getText();
	    model = CustomLogicModel.getModelWithNameOrCopy(modelName, model);
	    setupPins();
	    allocNodes();
	    setPoints();
	    return;
	}
	if (n == 3)
	{
	    EditDialog editDialog = new EditDialog(model, sim);
	    CirSim.customLogicEditDialog = editDialog;
	    editDialog.show();
	    return;
	}
	
	super.setEditValue(n, ei);
    }
    
    int getDumpType() { return 208; }

    void getInfo(String arr[]) {
	super.getInfo(arr);
	arr[0] = model.infoText;
    }
}
