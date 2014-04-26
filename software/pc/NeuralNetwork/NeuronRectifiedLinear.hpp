#include "Neuron.hpp"

class NeuronRectifiedLinear : public Neuron
{
public:
	NeuronRectifiedLinear(unsigned numOutputs, unsigned index, unsigned layerNum, unsigned threshold);

private:
	double transfer(double x)
	{
		return (x > m_threshold) ? x : 0;
	}

    double transferDerivative(double x)
	{
		return (x == 0) ? 1 : 0;
	}

	double m_threshold;
};

NeuronRectifiedLinear::NeuronRectifiedLinear(unsigned numOutputs, unsigned index, unsigned layerNum, unsigned threshold):
			Neuron(numOutputs, index, layerNum)
{
	m_threshold = threshold;
}