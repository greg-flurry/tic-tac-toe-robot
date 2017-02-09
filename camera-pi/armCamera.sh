# !/bin/bash

# check for a white balance parameter
if [ $# -eq 0 ]
then
	# use default white balance
	#echo "got no parameter"
	wb=d
elif [ $# -ge 1 ]
then 
	# check for the proper option (ignore any other parameters)
	if [ $1 = "i" ] || [ $1 = "f" ] || [ $1 = "s" ] || [ $1 = "d" ] 
	then
		# got a valid white balance
		wb=$1
	else	
		echo "armCamera Help ..."
		echo "Starts the image capture process"
		echo ""
		echo "Syntax 'bash armCamera.sh [parmater]'"
		echo "One parameter optional, the desired white balance"
		echo "-- i = incandescent"
		echo "-- f = fluorescent"		
		echo "-- s = sunlight"
		echo "-- d = shade [default]"
		exit 0
	fi
fi

#echo "the white balance to be used is " $wb

python3 ~/Robotics/Arm-Camera/final/image-sender-param.py -w $wb
