#!/bin/bash

PROFILE="--profile ecomm"

# list-subnet-routetables
# Single Subnet allowed
function listRouteTablesSubnet() {
    aws ec2 describe-route-tables --filter Name=association.subnet-id,Values=$SUBNET_ID --query "RouteTables[*].Routes" $PROFILE | jq .
}

# subnet-is-public
# Single Subnet allowed
function checkSubnetIsPublic() {
    clear
    echo " "
    echo " ===================================================================="
    echo " Subnet is Public $SUBNET_ID" | GREP_COLOR='01;32' egrep --color=always $SUBNET_ID
    echo " ===================================================================="
    echo " "
    aws ec2 describe-route-tables --filter Name=association.subnet-id,Values=$SUBNET_ID --query "RouteTables[].Routes" $PROFILE | jq '.[] | .[] | select(.DestinationCidrBlock | contains("0.0.0.0/0"))'
}

# list-subnets-vpc
# Single VPC Allowed
function listAllSubnetsVPC() {
    aws ec2 describe-subnets --filters Name=vpc-id,Values=$VPC_ID --query "Subnets[*].[SubnetId,AvailabilityZone,CidrBlock]"  $PROFILE | jq .
}

# list-cidr-subnets
# Multiple Subnets allowed
function listCidrSubnet() {
    aws ec2 describe-subnets --subnet-ids  $SUBNETS_ID --query Subnets[*].[SubnetId,AvailabilityZone,CidrBlock] $PROFILE | jq .
}

showCommands() {
   echo " List of all available commands:"  | GREP_COLOR='01;35' egrep --color=always 'List of all available commands:' 
   echo "   - subnet-is-public" | GREP_COLOR='01;32' egrep --color=always 'subnet-is-public' 
   echo "   - list-subnets-vpc" | GREP_COLOR='01;32' egrep --color=always 'list-subnets-vpc' 
   echo "   - list-cidr-subnets" | GREP_COLOR='01;32' egrep --color=always 'list-cidr-subnets' 
   echo "   - list-subnet-routetables" | GREP_COLOR='01;32' egrep --color=always 'list-subnet-routetables' 
   echo " "
}

showUsage() {
   clear
   echo " ===================================================================="
   echo " usage:" | GREP_COLOR='01;35' egrep --color=always 'usage:' 
   echo " "
   echo "  :~ run.sh subnet-is-public subnet-05407cde08f575e3d" | GREP_COLOR='01;32' egrep --color=always 'subnet-is-public' 
   echo "  :~ run.sh list-cidr-subnets subnet-05407cde08f575e3d" | GREP_COLOR='01;32' egrep --color=always 'list-cidr-subnets' 
   echo "  :~ run.sh list-cidr-subnets 'subnet-05407cde08f575e3d subnet-01a1e8590b13125d6'" | GREP_COLOR='01;32' egrep --color=always 'list-cidr-subnets' 
   echo "  :~ run.sh list-subnet-routetables subnet-05407cde08f575e3d" | GREP_COLOR='01;32' egrep --color=always 'list-subnet-routetables' 

   echo " "
   echo "  or " | GREP_COLOR='01;35' egrep --color=always 'or' 
   echo " "
   echo "  Set parameter as environment variables:" | GREP_COLOR='01;35' egrep --color=always 'Set parameter as environment variables:' 
   echo "   :~ export SUBNET_ID = [value] "
   echo "   :~ export SUBNES_ID = [value] "
   echo "   :~ export VPC_ID    = [value] "
   echo "  And then:" | GREP_COLOR='01;35' egrep --color=always 'And then:' 
   echo "   :~ run.sh subnet-is-public" | GREP_COLOR='01;32' egrep --color=always 'subnet-is-public'
   echo "   :~ run.sh list-cidr-subnets" | GREP_COLOR='01;32' egrep --color=always 'list-cidr-subnets' 
   echo " "
   showCommands
   echo " "
   echo " ===================================================================="
   echo " "
}

if [ -z $1 ]; then
   showUsage
   exit
fi

if [ "$1" = "-l" ] || [ "$1" = "-h" ] || [ "$1" = "list" ]; then
   clear
   echo " "
   echo " ===================================================================="
   showCommands
   echo " ===================================================================="
   echo " "
   exit
fi

if [ "$1" = "subnet-is-public" ]; then
   if [ -z $2 ]; then
      if [[ -z "${SUBNET_ID}" ]]; then
         showUsage
         exit
      else 
         SUBNET_ID="${SUBNET_ID}"
         echo "SubnetId...: $SUBNET_ID"
         checkSubnetIsPublic
         exit
      fi
   else       
      SUBNET_ID=$2
      echo "SubnetId...: $SUBNET_ID"
      checkSubnetIsPublic
      exit
   fi
fi


if [ "$1" = "list-subnets-vpc" ]; then
   if [ -z $2 ]; then
      if [[ -z "${VPC_ID}" ]]; then
         showUsage
         exit
      else 
         VPC_ID="${VPC_ID}"
         echo "VPC_ID...: $VPC_ID"
         listAllSubnetsVPC
         exit
      fi
   else
      VPC_ID=$2
      echo "VPC_ID...: $VPC_ID"
      listAllSubnetsVPC
      exit
   fi
fi

if [ "$1" = "list-cidr-subnets" ]; then
   if [ -z "$2" ]; then
      if [[ -z "${SUBNETS_ID}" ]]; then
         showUsage
         exit
      else 
         SUBNETS_ID="${SUBNETS_ID}"
         echo "SUBNETS_ID...: $SUBNETS_ID"
         listCidrSubnet
         exit
      fi
   else
      SUBNETS_ID=$2
      echo "SUBNETS_ID...: $SUBNETS_ID"
      listCidrSubnet
      exit
   fi
fi


if [ "$1" = "list-subnet-routetables" ]; then
   if [ -z $2 ]; then
      if [[ -z "${SUBNET_ID}" ]]; then
         showUsage
         exit
      else 
         SUBNET_ID="${SUBNET_ID}"
         echo "SubnetId...: $SUBNET_ID"
         listRouteTablesSubnet
         exit
      fi
   else       
      SUBNET_ID=$2
      echo "SubnetId...: $SUBNET_ID"
      listRouteTablesSubnet
      exit
   fi
fi


showUsage
echo " -----> No command informed! "
echo " "
